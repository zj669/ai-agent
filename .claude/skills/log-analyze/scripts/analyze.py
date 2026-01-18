#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
日志分析工具 (Standalone)
用于自动解析 Maven/Java 构建日志，精确定位错误位置并生成分析报告。

功能特点：
1. 自动检测文件编码（GBK, UTF-8等），解决 Windows 中文乱码问题
2. 智能识别 Maven 错误、编译失败、测试失败等多种错误类型
3. 生成 Bug_Report.md，包含关键堆栈和修复建议
4. 支持 Grep 模式，无需读取全文即可查看关键日志片段（防截断）

用法：
  # 生成分析报告 (标准模式)
  python analyze.py <日志路径> <报告路径>

  # 搜索关键字 (交互模式)
  python analyze.py <日志路径> --grep "NullPointerException" -c 10
"""

import re
import sys
import argparse
from pathlib import Path
from typing import List, Dict, Tuple, Optional, Any
from dataclasses import dataclass
from datetime import datetime

# 尝试导入 chardet 用于编码检测
try:
    import chardet
    CHARDET_AVAILABLE = True
except ImportError:
    CHARDET_AVAILABLE = False


def safe_print(msg: str, file=None):
    """
    安全打印函数，处理 Windows GBK 控制台无法显示 emoji 的问题
    """
    try:
        print(msg, file=file)
    except UnicodeEncodeError:
        import re
        clean_msg = re.sub(r'[^\x00-\x7F]+', '', msg)
        print(clean_msg, file=file)


@dataclass
class ErrorEntry:
    """错误条目数据结构（增强版）"""
    line_number: int
    error_type: str
    content: str
    context_lines: List[str]
    stack_trace: List[str]
    # 新增字段
    file_path: Optional[str] = None
    file_name: Optional[str] = None
    line_num: Optional[int] = None
    column_num: Optional[int] = None


class LogAnalyzer:
    """日志分析器"""
    
    # 重构：摒弃错误字典模式，采用内容驱动识别
    # 堆栈终止信号模式（用于贪婪提取）
    TERMINATION_PATTERNS = [
        re.compile(r'^\[INFO\]'),
        re.compile(r'^\[DEBUG\]'),
        re.compile(r'^\[WARN\]'),
        re.compile(r'^\[TRACE\]'),
        # Maven构建阶段标记
        re.compile(r'^-+.*maven.*-+'),
        # 测试结果
        re.compile(r'Tests run:.*Failures:')
    ]
    
    def __init__(self, log_path: str, max_errors: int = 5, context_lines: int = 20, encoding: Optional[str] = None):
        self.log_path = Path(log_path)
        self.max_errors = max_errors
        self.context_lines = context_lines
        self.encoding = encoding
        self.detected_encoding: Optional[str] = None
        self.errors: List[ErrorEntry] = []
        
    def _detect_encoding(self) -> str:
        """自动检测文件编码"""
        if self.detected_encoding:
            return self.detected_encoding
            
        if self.encoding:
            self.detected_encoding = self.encoding
            return self.encoding
        
        try:
            with open(self.log_path, 'rb') as f:
                raw_data = f.read(10000)
            
            if CHARDET_AVAILABLE and raw_data:
                result = chardet.detect(raw_data)
                detected = result.get('encoding', 'utf-8')
                confidence = result.get('confidence', 0)
                
                if confidence > 0.7:
                    if detected and detected.upper() in ['GB2312', 'GB18030']:
                        detected = 'GBK'
                    safe_print(f"[*] 检测到编码: {detected} (置信度: {confidence:.2%})")
                    self.detected_encoding = detected
                    return detected
            
            # 备用检测
            common_encodings = ['utf-8', 'gbk', 'gb2312', 'gb18030', 'utf-16']
            for enc in common_encodings:
                try:
                    raw_data.decode(enc)
                    safe_print(f"[*] 使用编码: {enc} (备用检测)")
                    self.detected_encoding = enc
                    return enc
                except (UnicodeDecodeError, LookupError):
                    continue
            
            safe_print(f"[!] 无法确定编码，使用 UTF-8")
            self.detected_encoding = 'utf-8'
            return 'utf-8'
            
        except Exception as e:
            safe_print(f"[!] 编码检测失败: {e}，使用 UTF-8")
            self.detected_encoding = 'utf-8'
            return 'utf-8'

    def read_lines(self) -> List[str]:
        """读取所有行（使用检测到的编码），并标准化换行符"""
        if not self.log_path.exists():
            raise FileNotFoundError(f"日志文件不存在: {self.log_path}")
            
        encoding = self._detect_encoding()
        try:
            with open(self.log_path, 'rb') as f:
                raw_data = f.read()
            
            # 标准化换行符：有些日志（如 Maven 进度条）使用单独的 \r 或混合换行
            # 1. 先将 CRLF 转为 LF
            # 2. 再将剩余的单独 CR 转为 LF
            normalized = raw_data.replace(b'\r\n', b'\n').replace(b'\r', b'\n')
            text = normalized.decode(encoding, errors='replace')
            lines = text.split('\n')
            
            # 保留尾部空行信息（与 readlines 行为一致）
            if lines and lines[-1] == '':
                lines = lines[:-1]
            
            return [line + '\n' for line in lines]
            
        except Exception as e:
            safe_print(f"[!] 读取失败: {e}，重试 UTF-8")
            with open(self.log_path, 'r', encoding='utf-8', errors='ignore') as f:
                return f.readlines()

    def analyze(self) -> List[ErrorEntry]:
        """完整的日志分析流程（重构版）"""
        lines = self.read_lines()
        self.errors = []
        
        i = 0
        while i < len(lines) and len(self.errors) < self.max_errors:
            line = lines[i]
            
            if self._is_error_indicator(line):
                # 记录错误起始位置
                error_start = i
                
                # 提取基本信息
                error_type = self._identify_error_type_from_content(line)
                location_info = self._extract_file_location(line)
                context_lines = self._extract_context_with_boundaries(lines, i)
                
                # 贪婪提取完整堆栈（关键改进）
                stack_trace, next_process_pos = self._extract_greedy_stack_trace(lines, i)
                
                # 创建错误条目
                error_entry = ErrorEntry(
                    line_number=error_start + 1,
                    error_type=error_type,
                    content=line.rstrip(),
                    context_lines=context_lines,
                    stack_trace=stack_trace,
                    file_path=location_info.get('file_path'),
                    file_name=location_info.get('file_name'),
                    line_num=location_info.get('line_number'),
                    column_num=location_info.get('column_number')
                )
                
                self.errors.append(error_entry)
                
                # 安全跳转到下一个处理位置
                i = next_process_pos
            else:
                i += 1
        
        return self.errors
    
    def _extract_file_location(self, error_line: str) -> Dict[str, Any]:
        """从错误行中提取文件位置信息"""
        location_info = {
            'file_path': None,
            'line_number': None,
            'column_number': None,
            'file_name': None
        }
        
        # 使用现有的正则表达式模式
        file_match = re.search(r'\[(?:ERROR|WARNING)\]\s+([A-Za-z]:[/\\].*?\.java):\[?(\d+)[,:](\d+)\]?', error_line)
        if file_match:
            location_info['file_path'] = file_match.group(1)
            location_info['file_name'] = Path(file_match.group(1)).name
            location_info['line_number'] = int(file_match.group(2))
            location_info['column_number'] = int(file_match.group(3))
        
        return location_info
    
    def _extract_context_with_boundaries(self, lines: List[str], error_start: int) -> List[str]:
        """提取带有智能边界的上下文信息"""
        context = []
        
        # 向前搜索上下文（最多20行）
        pre_start = max(0, error_start - 20)
        for i in range(error_start - 1, pre_start - 1, -1):
            line = lines[i].rstrip()
            # 遇到明显的分隔符或构建阶段标记时停止
            if '[INFO]' in line and ('---' in line or 'Building' in line):
                break
            context.insert(0, f"[{i+1:4d}] {line}")
        
        # 添加错误行本身
        context.append(f"[{error_start+1:4d}] {lines[error_start].rstrip()}")
        
        # 向后搜索上下文（最多20行）
        post_end = min(len(lines), error_start + 21)
        for i in range(error_start + 1, post_end):
            line = lines[i].rstrip()
            # 遇到新的错误开始时停止
            if self._is_error_indicator(line) and i > error_start + 1:
                break
            context.append(f"[{i+1:4d}] {line}")
        
        return context

    def grep_search(self, keyword: str, max_matches: int = 10):
        """
        搜索关键字并打印上下文（前后各 context_lines 行）
        """
        lines = self.read_lines()
        total_lines = len(lines)
        safe_print(f"[*] 正在搜索: '{keyword}' (上下文: 前后各 {self.context_lines} 行, 最大匹配: {max_matches})")
        safe_print(f"[*] 日志文件共 {total_lines} 行")
        safe_print("")
        
        matches_found = 0
        matched_ranges = set()  # 记录已输出的行号范围，避免重复
        
        i = 0
        while i < total_lines and matches_found < max_matches:
            if keyword in lines[i] and i not in matched_ranges:
                matches_found += 1
                
                # 计算前后上下文范围
                start = max(0, i - self.context_lines)
                end = min(total_lines, i + self.context_lines + 1)
                
                safe_print("=" * 70)
                safe_print(f"匹配 #{matches_found} @ 行 {i+1}")
                safe_print("-" * 70)
                
                # 打印前文
                for j in range(start, i):
                    if j not in matched_ranges:
                        safe_print(f"  {j+1:5d} | {lines[j].rstrip()}")
                        matched_ranges.add(j)
                
                # 打印匹配行（高亮）
                safe_print(f"> {i+1:5d} | {lines[i].rstrip()}")
                matched_ranges.add(i)
                
                # 打印后文
                for j in range(i + 1, end):
                    if j not in matched_ranges:
                        safe_print(f"  {j+1:5d} | {lines[j].rstrip()}")
                        matched_ranges.add(j)
                
                safe_print("")
                
                # 跳过已打印的后文范围，避免重复匹配
                i = end
            else:
                i += 1
        
        if matches_found == 0:
            safe_print("[!] 未找到匹配项")
        else:
            safe_print("=" * 70)
            safe_print(f"[*] 搜索完成，共找到 {matches_found} 处匹配")

    def _identify_error_type_from_content(self, error_content: str) -> str:
        """基于内容动态识别错误类型（摒弃字典模式）"""
        content_lower = error_content.lower()
        
        # 编译错误识别
        if 'cannot find symbol' in content_lower:
            return 'COMPILATION_SYMBOL_ERROR'
        elif 'package' in content_lower and 'does not exist' in content_lower:
            return 'COMPILATION_PACKAGE_ERROR'
        elif 'compilation failure' in content_lower or 'compilation error' in content_lower:
            return 'COMPILATION_GENERAL_FAILURE'
        elif 'incompatible types' in content_lower:
            return 'COMPILATION_TYPE_ERROR'
        elif ';' in error_content and 'expected' in content_lower:
            return 'COMPILATION_SYNTAX_ERROR'
        
        # 运行时异常识别
        exception_patterns = [
            ('nullpointerexception', 'RUNTIME_NULL_POINTER'),
            ('classcastexception', 'RUNTIME_CLASS_CAST'),
            ('arrayindexoutofboundsexception', 'RUNTIME_ARRAY_INDEX'),
            ('illegalargumentexception', 'RUNTIME_ILLEGAL_ARGUMENT'),
            ('illegalstateexception', 'RUNTIME_ILLEGAL_STATE'),
            ('numberformatexception', 'RUNTIME_NUMBER_FORMAT')
        ]
        
        for pattern, error_type in exception_patterns:
            if pattern in content_lower:
                return error_type
        
        # 构建错误识别
        if 'dependencyresolutionexception' in content_lower or 'could not resolve dependencies' in content_lower:
            return 'BUILD_DEPENDENCY_ERROR'
        elif 'failed to execute goal' in content_lower:
            return 'BUILD_PLUGIN_FAILURE'
        elif 'invalid configuration' in content_lower or 'invalid plugin descriptor' in content_lower:
            return 'BUILD_CONFIG_ERROR'
        elif 'build failure' in content_lower:
            return 'BUILD_GENERAL_FAILURE'
        
        # 默认分类
        if 'exception' in content_lower:
            return 'RUNTIME_GENERIC_EXCEPTION'
        elif 'error' in content_lower:
            return 'GENERIC_ERROR'
        else:
            return 'UNKNOWN_ERROR'
    
    def _is_termination_signal(self, line: str, current_idx: int, start_idx: int) -> bool:
        """判断是否是堆栈提取的终止信号"""
        # 避免在错误起始行就终止
        if current_idx == start_idx:
            return False
            
        # 检查明确的终止信号
        if line.startswith('[INFO]') or line.startswith('[DEBUG]') or line.startswith('[WARN]'):
            return True
        
        # 新的错误标记（但排除堆栈中的Caused by）
        if ('[ERROR]' in line and 
            'Exception' not in line and 
            'Caused by' not in line and
            'at ' not in line):
            return True
            
        # Maven构建阶段标记（更严格的匹配）
        if '---' in line and ('maven' in line.lower() or 'plugin' in line.lower() or 'executing' in line.lower()):
            return True
            
        # 测试结果汇总
        if 'Tests run:' in line and 'Failures:' in line:
            return True
            
        return False
    
    def _extract_greedy_stack_trace(self, lines: List[str], start_idx: int) -> Tuple[List[str], int]:
        """
        贪婪模式堆栈提取 - 吞噬所有非明确终止信号的行
        返回: (堆栈行列表, 下一个处理位置)
        """
        stack_trace = []
        i = start_idx
        max_scan = min(len(lines), start_idx + 100)  # 防止过度扫描
        
        while i < max_scan:
            line = lines[i].rstrip()
            
            # 空行处理 - 作为堆栈的一部分或分隔符
            if not line.strip():
                stack_trace.append(line)
                i += 1
                continue
                
            # 关键：检查是否是终止信号
            if self._is_termination_signal(line, i, start_idx):
                break
                
            # 吞噬当前行
            stack_trace.append(line)
            i += 1
        
        return stack_trace, i
    
    def _is_error_indicator(self, line: str) -> bool:
        """判断是否是错误指示行"""
        indicators = [
            '[ERROR]' in line,
            'FAILURE' in line.upper(),
            'Exception' in line,
            'Error:' in line,
            'Caused by:' in line
        ]
        return any(indicators)
    
    def generate_complete_report(self) -> str:
        """生成包含所有错误的完整报告"""
        if not self.errors:
            return "[OK] 未发现错误！"
        
        report_sections = [
            "# 完整错误分析报告",
            f"分析时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            f"发现错误总数: {len(self.errors)} 个",
            "=" * 60,
            ""
        ]
        
        # 为每个错误生成详细报告
        for idx, error in enumerate(self.errors, 1):
            error_section = self._generate_error_details(idx, error)
            report_sections.extend(error_section)
            report_sections.append("")  # 分隔空行
        
        return "\n".join(report_sections)
    
    def _generate_error_details(self, index: int, error: ErrorEntry) -> List[str]:
        """生成单个错误的详细信息"""
        details = [
            f"## 🔴 错误 #{index}",
            f"- **类型**: {error.error_type}",
            f"- **位置**: 第 {error.line_number} 行"
        ]
        
        # 文件位置信息
        if error.file_name:
            location = error.file_name
            if error.line_num:
                location += f":{error.line_num}"
                if error.column_num:
                    location += f":{error.column_num}"
            details.append(f"- **文件**: {location}")
        
        # 错误内容
        details.extend([
            "",
            "**错误详情:**",
            "```",
            error.content,
            "```"
        ])
        
        # 完整堆栈跟踪
        if error.stack_trace:
            details.extend([
                "",
                "**完整堆栈跟踪:**",
                "```"
            ])
            details.extend(error.stack_trace)
            details.append("```")
        
        # 关键上下文
        if error.context_lines:
            # 提取错误行周围的上下文
            error_line_marker = f"[{error.line_number:4d}]"
            context_window = self._extract_context_window(error.context_lines, error_line_marker)
            if context_window:
                details.extend([
                    "",
                    "**相关上下文:**",
                    "```"
                ])
                details.extend(context_window)
                details.append("```")
        
        details.append("-" * 40)
        return details
    
    def _extract_context_window(self, context_lines: List[str], error_marker: str) -> List[str]:
        """从上下文中提取关键窗口"""
        error_idx = None
        for i, line in enumerate(context_lines):
            if error_marker in line:
                error_idx = i
                break
        
        if error_idx is not None:
            start = max(0, error_idx - 2)
            end = min(len(context_lines), error_idx + 3)
            return context_lines[start:end]
        return []

    def generate_bug_report(self) -> str:
        """生成简洁 Bug 报告"""
        if not self.errors:
            return "[OK] 未发现错误！"
        
        error = self.errors[0]
        content = error.content + "\n" + "\n".join(error.context_lines)
        
        failure_type = "Unknown"
        if 'cannot find symbol' in content: failure_type = "SymbolNotFound"
        elif 'package' in content and 'does not exist' in content: failure_type = "PackageNotFound"
        elif 'compilation failure' in content.lower(): failure_type = "CompilationError"
        elif 'Exception' in content: failure_type = "RuntimeException"
        
        location = "Unknown"
        file_match = re.search(r'\[(ERROR|WARNING)\]\s+([A-Za-z]:[\\/].*?\.java):\[(\d+)[,:](\d+)\]', content)
        if file_match:
            location = f"{Path(file_match.group(2)).name}:[{file_match.group(3)},{file_match.group(4)}]"
            
        key_trace = error.content
        if error.context_lines:
            key_trace += "\n" + "\n".join(error.context_lines[:5])
            
        return f"""
> **[Bug Report]**
> * **Failure Type**: {failure_type}
> * **Location**: {location}
> * **Key Trace**:
> ```text
{key_trace}
> ```
> * **Root Cause**: {self._infer_root_cause(error)}
"""

    def _analyze_error(self, error: ErrorEntry) -> List[str]:
        analysis = []
        content = error.content + "\n" + "\n".join(error.context_lines)
        
        if 'cannot find symbol' in content:
            analysis.append("[X] 错误类型: 符号未找到")
            analysis.append("[*] 修复建议: 检查 import、拼写或依赖")
        elif 'package' in content and 'does not exist' in content:
            analysis.append("[X] 错误类型: 包不存在")
            analysis.append("[*] 修复建议: 检查 Maven 依赖")
        elif 'NullPointerException' in content:
            analysis.append("[X] 错误类型: 空指针异常")
        elif 'incompatible types' in content:
            analysis.append("[X] 错误类型: 类型不匹配")
        
        file_match = re.search(r'\[(ERROR|WARNING)\]\s+([A-Za-z]:[\\/].*?\.java):\[(\d+)[,:](\d+)\]', content)
        if file_match:
            analysis.append(f"[*] 位置: {Path(file_match.group(2)).name}:[{file_match.group(3)},{file_match.group(4)}]")
            
        return analysis

    def _infer_root_cause(self, error: ErrorEntry) -> str:
        content = error.content.lower()
        if 'cannot find symbol' in content: return "缺少类/包导入或拼写错误"
        elif 'package' in content: return "Maven 依赖缺失"
        return "代码语法错误或逻辑异常"

    def _save_report(self, report: str, path: str):
        output_file = Path(path)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(report)
        safe_print(f"[+] 报告已保存到: {output_file}")


def main():
    parser = argparse.ArgumentParser(description='Java/Maven 日志分析与搜索工具')
    parser.add_argument('log_path', help='日志文件路径')
    parser.add_argument('report_path', nargs='?', help='报告输出路径 (仅在分析模式下需要)')
    parser.add_argument('--grep', help='搜索关键字 (Grep 模式)')
    parser.add_argument('-c', '--context', type=int, default=20, help='上下文行数 (默认: 20)')
    parser.add_argument('-n', '--max-matches', type=int, default=10, help='最大匹配数量 (默认: 10，防止输出过多)')
    
    args = parser.parse_args()
    
    log_path = Path(args.log_path).absolute()
    if not log_path.exists():
        safe_print(f"[X] 错误: 日志文件不存在 {log_path}")
        sys.exit(1)

    try:
        analyzer = LogAnalyzer(str(log_path), context_lines=args.context)
        
        # 模式 1: Grep 搜索 (交互模式)
        if args.grep:
            analyzer.grep_search(args.grep, max_matches=args.max_matches)
            return

        # 模式 2: 完整分析 (报告模式)
        if not args.report_path:
            # 如果没有 grep 也没有 report_path，提示错误
            safe_print("[!] 必须指定报告输出路径，或使用 --grep 进行搜索")
            print("用法: python analyze.py <日志路径> <报告路径>")
            sys.exit(1)
            
        report_path = Path(args.report_path).absolute()
        safe_print(f"[*] 正在分析日志: {log_path.name}")
        errors = analyzer.analyze()
        
        if errors:
            safe_print(f"[+] 发现 {len(errors)} 个错误\n")
            report = analyzer.generate_complete_report()
            analyzer._save_report(report, str(report_path))
            print("\n" + report + "\n")
        else:
            safe_print("[!] 未发现明显错误")
            
    except Exception as e:
        safe_print(f"[X] 执行出错: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()
