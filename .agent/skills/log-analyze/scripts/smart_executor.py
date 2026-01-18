#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能命令执行代理
统一入口处理命令执行、日志记录和自动分析

核心功能：
1. 命令执行 + 日志记录一体化
2. 自动路径管理和命名
3. 内置常用命令模板
4. 结果缓存和快速检索
5. 与现有 analyze.py 无缝集成
"""

import argparse
import subprocess
import sys
import os
import json
import hashlib
import time
from pathlib import Path
from datetime import datetime
from typing import Optional, Dict, List, Tuple

# 导入现有分析工具
sys.path.append(os.path.dirname(__file__))
from analyze import LogAnalyzer


class SmartExecutor:
    def __init__(self, config_path: Optional[str] = None):
        self.config = self._load_config(config_path)
        self.cache_dir = Path(".business/cache")
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        
    def _load_config(self, config_path: Optional[str]) -> Dict:
        """加载配置文件"""
        if not config_path:
            config_path = Path(__file__).parent.parent / "config" / "aliases.json"
        
        default_config = {
            "defaults": {
                "log_dir": ".business/{feature}/executelogs/",
                "report_dir": ".business/{feature}/"
            },
            "templates": {
                "maven-build": "mvn clean install",
                "maven-test": "mvn test",
                "docker-up": "docker-compose up",
                "docker-down": "docker-compose down",
                "git-status": "git status",
                "npm-install": "npm install",
                "npm-run-dev": "npm run dev"
            }
        }
        
        if Path(config_path).exists():
            try:
                with open(config_path, 'r', encoding='utf-8') as f:
                    user_config = json.load(f)
                # 合并配置
                merged = default_config.copy()
                merged.update(user_config)
                if 'defaults' in user_config:
                    merged['defaults'].update(user_config['defaults'])
                if 'templates' in user_config:
                    merged['templates'].update(user_config['templates'])
                return merged
            except Exception as e:
                print(f"[!] 配置文件加载失败: {e}，使用默认配置")
        
        return default_config
    
    def _generate_paths(self, feature: str, command_hash: str) -> Tuple[Path, Path]:
        """自动生成日志和报告路径"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 生成日志路径
        log_dir_template = self.config["defaults"]["log_dir"]
        log_dir = Path(log_dir_template.format(feature=feature))
        log_dir.mkdir(parents=True, exist_ok=True)
        log_path = log_dir / f"exec_{timestamp}_{command_hash[:8]}.log"
        
        # 生成报告路径
        report_dir_template = self.config["defaults"]["report_dir"]
        report_dir = Path(report_dir_template.format(feature=feature))
        report_dir.mkdir(parents=True, exist_ok=True)
        report_path = report_dir / f"analysis_{timestamp}_{command_hash[:8]}.md"
        
        return log_path, report_path
    
    def _hash_command(self, command: str) -> str:
        """生成命令哈希值用于标识"""
        return hashlib.md5(command.encode()).hexdigest()
    
    def execute_with_logging(self, command: str, feature: str = "default") -> Dict:
        """
        执行命令并记录日志
        
        Args:
            command: 要执行的命令
            feature: 功能标识，用于路径组织
            
        Returns:
            dict: 执行结果信息
        """
        command_hash = self._hash_command(command)
        log_path, report_path = self._generate_paths(feature, command_hash)
        
        print(f"[*] 执行命令: {command}")
        print(f"[*] 日志路径: {log_path}")
        print(f"[*] 报告路径: {report_path}")
        
        try:
            # 执行命令并重定向输出
            cmd_with_redirect = f'cmd /c "{command} > {log_path} 2>&1"'
            result = subprocess.run(cmd_with_redirect, shell=True)
            
            execution_info = {
                "command": command,
                "exit_code": result.returncode,
                "log_path": str(log_path),
                "report_path": str(report_path),
                "feature": feature,
                "timestamp": datetime.now().isoformat(),
                "success": result.returncode == 0
            }
            
            # 保存执行信息到缓存
            self._cache_execution(execution_info)
            
            if result.returncode != 0:
                print(f"[!] 命令执行失败 (退出码: {result.returncode})")
            else:
                print(f"[+] 命令执行成功")
                
            return execution_info
            
        except Exception as e:
            error_info = {
                "command": command,
                "error": str(e),
                "log_path": str(log_path),
                "feature": feature,
                "timestamp": datetime.now().isoformat(),
                "success": False
            }
            print(f"[X] 执行出错: {e}")
            return error_info
    
    def auto_analyze(self, execution_info: Dict) -> Optional[str]:
        """
        自动分析执行结果
        
        Args:
            execution_info: execute_with_logging返回的执行信息
            
        Returns:
            str: 分析报告内容，如果成功的话
        """
        if execution_info["success"]:
            print("[*] 命令执行成功，无需分析")
            return None
            
        log_path = execution_info["log_path"]
        report_path = execution_info["report_path"]
        
        if not Path(log_path).exists():
            print(f"[!] 日志文件不存在: {log_path}")
            return None
            
        print(f"[*] 开始分析日志: {log_path}")
        
        try:
            # 使用现有分析工具
            analyzer = LogAnalyzer(log_path)
            errors = analyzer.analyze()
            
            if errors:
                print(f"[+] 发现 {len(errors)} 个错误")
                report = analyzer.generate_bug_report()
                
                # 保存报告
                analyzer._save_report(report, report_path)
                print(f"[+] 分析报告已保存: {report_path}")
                return report
            else:
                print("[*] 未发现明显错误")
                return "[OK] 未发现明显错误"
                
        except Exception as e:
            print(f"[X] 分析过程出错: {e}")
            return None
    
    def _cache_execution(self, execution_info: Dict):
        """缓存执行信息"""
        cache_file = self.cache_dir / "executions.json"
        
        try:
            if cache_file.exists():
                with open(cache_file, 'r', encoding='utf-8') as f:
                    cache = json.load(f)
            else:
                cache = []
            
            # 添加新记录
            cache.append(execution_info)
            
            # 保持最近100条记录
            if len(cache) > 100:
                cache = cache[-100:]
            
            with open(cache_file, 'w', encoding='utf-8') as f:
                json.dump(cache, f, indent=2, ensure_ascii=False)
                
        except Exception as e:
            print(f"[!] 缓存保存失败: {e}")
    
    def get_recent_executions(self, limit: int = 10) -> List[Dict]:
        """获取最近的执行记录"""
        cache_file = self.cache_dir / "executions.json"
        
        if not cache_file.exists():
            return []
            
        try:
            with open(cache_file, 'r', encoding='utf-8') as f:
                cache = json.load(f)
            return cache[-limit:] if len(cache) >= limit else cache
        except Exception as e:
            print(f"[!] 缓存读取失败: {e}")
            return []
    
    def get_failed_executions(self, limit: int = 5) -> List[Dict]:
        """获取最近失败的执行记录"""
        recent = self.get_recent_executions(limit * 3)  # 多获取一些以筛选
        failed = [item for item in recent if not item.get("success", True)]
        return failed[-limit:] if len(failed) >= limit else failed


def main():
    parser = argparse.ArgumentParser(
        description='智能命令执行代理 - 一体化命令执行和日志分析',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  # 直接执行并自动分析
  python smart_executor.py --run "mvn clean install" --feature my_project --auto-analyze
  
  # 使用预设模板
  python smart_executor.py --template maven-build --feature user_auth
  
  # 查看最近的失败执行
  python smart_executor.py --recent-errors 5
  
  # 查看执行历史
  python smart_executor.py --history 10
        """
    )
    
    # 执行模式
    parser.add_argument('--run', help='直接执行指定命令')
    parser.add_argument('--template', help='使用预设模板执行')
    
    # 配置参数
    parser.add_argument('--feature', default='default', help='功能标识 (用于路径组织)')
    parser.add_argument('--config', help='配置文件路径')
    
    # 分析选项
    parser.add_argument('--auto-analyze', action='store_true', help='自动分析失败的执行')
    
    # 查询模式
    parser.add_argument('--recent-errors', type=int, metavar='N', help='显示最近N个失败执行')
    parser.add_argument('--history', type=int, metavar='N', help='显示最近N个执行历史')
    
    args = parser.parse_args()
    
    executor = SmartExecutor(args.config)
    
    # 查询模式
    if args.recent_errors:
        failed = executor.get_failed_executions(args.recent_errors)
        if failed:
            print(f"最近 {len(failed)} 个失败执行:")
            for i, exec_info in enumerate(failed, 1):
                print(f"{i}. [{exec_info['timestamp']}] {exec_info['command']}")
                print(f"   日志: {exec_info['log_path']}")
                print(f"   状态: {'失败' if not exec_info['success'] else '成功'}")
                print()
        else:
            print("没有找到失败的执行记录")
        return
    
    if args.history:
        history = executor.get_recent_executions(args.history)
        if history:
            print(f"最近 {len(history)} 个执行记录:")
            for i, exec_info in enumerate(history, 1):
                status = "✅" if exec_info['success'] else "❌"
                print(f"{i}. {status} [{exec_info['timestamp']}] {exec_info['command']}")
        else:
            print("没有执行历史记录")
        return
    
    # 执行模式
    if args.run:
        command = args.run
    elif args.template:
        if args.template in executor.config["templates"]:
            command = executor.config["templates"][args.template]
        else:
            print(f"[X] 未知模板: {args.template}")
            print(f"可用模板: {', '.join(executor.config['templates'].keys())}")
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(1)
    
    # 执行命令
    execution_info = executor.execute_with_logging(command, args.feature)
    
    # 自动分析（如果失败且启用）
    if not execution_info["success"] and args.auto_analyze:
        report = executor.auto_analyze(execution_info)
        if report:
            print("\n=== 分析报告 ===")
            print(report)


if __name__ == '__main__':
    main()