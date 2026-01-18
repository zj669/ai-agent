#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能命令执行器 - Command Executor
专为安全执行系统命令设计，提供模板化和缓存功能

核心功能：
1. 安全命令执行 + 自动日志记录
2. 预设命令模板（Maven/NPM/Docker/Git等）
3. 执行历史缓存和快速检索
4. 路径自动管理和组织
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

# 导入自定义缓存管理器
sys.path.append(os.path.dirname(__file__))
from cache_manager import ExecutionCacheManager


class CommandExecutor:
    def __init__(self, config_path: Optional[str] = None):
        self.config = self._load_config(config_path)
        self.cache_manager = ExecutionCacheManager()

    def _load_config(self, config_path: Optional[str]) -> Dict:
        """加载配置文件"""
        if not config_path:
            config_path = Path(__file__).parent.parent / "config" / "aliases.json"

        default_config = {
            "defaults": {
                "log_dir": ".business/{feature}/executelogs/",
                "report_dir": ".business/{feature}/",
                "cache_ttl_hours": 24,
                "max_cache_size": 100
            },
            "templates": {
                "maven-build": "mvn clean install",
                "maven-test": "mvn test",
                "maven-package": "mvn package",
                "maven-clean": "mvn clean",
                "npm-install": "npm install",
                "npm-run-dev": "npm run dev"
            },
            "shortcuts": {
                "mb": "maven-build",
                "mt": "maven-test",
                "nb": "npm-run-build",
                "nt": "npm-run-test",
                "du": "docker-up",
                "dd": "docker-down",
                "gs": "git-status",
                "gd": "git-diff"
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
                if 'shortcuts' in user_config:
                    merged['shortcuts'].update(user_config['shortcuts'])
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

        # 生成报告路径（保留路径结构，供其他工具使用）
        report_dir_template = self.config["defaults"]["report_dir"]
        report_dir = Path(report_dir_template.format(feature=feature))
        report_dir.mkdir(parents=True, exist_ok=True)
        report_path = report_dir / f"execution_{timestamp}_{command_hash[:8]}.json"

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
                "success": result.returncode == 0,
                "command_hash": command_hash
            }

            # 保存执行信息到缓存
            self.cache_manager.save_execution(execution_info)

            if result.returncode != 0:
                print(f"[!] 命令执行失败 (退出码: {result.returncode})")
                print(f"[!] 请使用 log-analyze 技能分析日志: {log_path}")
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
                "success": False,
                "command_hash": command_hash
            }
            print(f"[X] 执行出错: {e}")
            return error_info

    def execute_template(self, template_id: str, feature: str = "default") -> Optional[Dict]:
        """
        使用预设模板执行命令

        Args:
            template_id: 模板ID或快捷键
            feature: 功能标识

        Returns:
            dict: 执行结果信息，如果模板不存在返回None
        """
        # 检查是否为快捷键
        if template_id in self.config.get("shortcuts", {}):
            actual_template = self.config["shortcuts"][template_id]
            print(f"[*] 使用快捷键 '{template_id}' -> '{actual_template}'")
        else:
            actual_template = template_id

        # 获取模板命令
        if actual_template in self.config["templates"]:
            command = self.config["templates"][actual_template]
            print(f"[*] 使用模板 '{actual_template}': {command}")
            return self.execute_with_logging(command, feature)
        else:
            print(f"[X] 未知模板: {template_id}")
            print(f"可用模板: {', '.join(self.config['templates'].keys())}")
            print(f"可用快捷键: {', '.join(self.config.get('shortcuts', {}).keys())}")
            return None

    def get_recent_executions(self, limit: int = 10) -> List[Dict]:
        """获取最近的执行记录"""
        return self.cache_manager.get_recent_executions(limit)

    def get_failed_executions(self, limit: int = 5) -> List[Dict]:
        """获取最近失败的执行记录"""
        return self.cache_manager.get_failed_executions(limit)


def main():
    parser = argparse.ArgumentParser(
        description='智能命令执行器 - 安全执行系统命令',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  # 直接执行命令
  python executor.py --run "mvn clean install" --feature my_project

  # 使用预设模板
  python executor.py --template maven-build --feature user_auth

  # 使用快捷键
  python executor.py --template mb --feature user_auth

  # 查看最近的失败执行
  python executor.py --recent-errors 5

  # 查看执行历史
  python executor.py --history 10

协作提示:
  执行失败时，会显示日志文件路径。可以使用 log-analyze 技能分析日志:
  /log-analyze <日志路径>
        """
    )

    # 执行模式
    execution_group = parser.add_argument_group('执行模式')
    execution_group.add_argument('--run', help='直接执行指定命令')
    execution_group.add_argument('--template', help='使用预设模板执行（支持快捷键）')

    # 配置参数
    config_group = parser.add_argument_group('配置参数')
    config_group.add_argument('--feature', default='default', help='功能标识 (用于路径组织)')
    config_group.add_argument('--config', help='配置文件路径')

    # 查询模式
    query_group = parser.add_argument_group('查询模式')
    query_group.add_argument('--recent-errors', type=int, metavar='N', help='显示最近N个失败执行')
    query_group.add_argument('--history', type=int, metavar='N', help='显示最近N个执行历史')
    query_group.add_argument('--list-templates', action='store_true', help='列出所有可用模板')

    args = parser.parse_args()

    executor = CommandExecutor(args.config)

    # 列出模板
    if args.list_templates:
        print("=== 可用模板 ===")
        for template_id, command in executor.config["templates"].items():
            print(f"  {template_id}: {command}")

        print("\n=== 可用快捷键 ===")
        shortcuts = executor.config.get("shortcuts", {})
        for shortcut, template_id in shortcuts.items():
            if template_id in executor.config["templates"]:
                command = executor.config["templates"][template_id]
                print(f"  {shortcut}: {template_id} -> {command}")
        return

    # 查询模式
    if args.recent_errors:
        failed = executor.get_failed_executions(args.recent_errors)
        if failed:
            print(f"最近 {len(failed)} 个失败执行:")
            for i, exec_info in enumerate(failed, 1):
                print(f"{i}. [{exec_info['timestamp']}] {exec_info['command']}")
                print(f"   日志: {exec_info['log_path']}")
                print(f"   退出码: {exec_info['exit_code']}")
                print()
        else:
            print("没有找到失败的执行记录")
        return

    if args.history:
        history = executor.get_recent_executions(args.history)
        if history:
            print(f"最近 {len(history)} 个执行记录:")
            for i, exec_info in enumerate(history, 1):
                status = "[成功]" if exec_info['success'] else "[失败]"
                print(f"{i}. {status} [{exec_info['timestamp']}] {exec_info['command']}")
        else:
            print("没有执行历史记录")
        return

    # 执行模式
    if args.run:
        command = args.run
        execution_info = executor.execute_with_logging(command, args.feature)

    elif args.template:
        execution_info = executor.execute_template(args.template, args.feature)
        if execution_info is None:
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(1)

    # 输出执行摘要
    print("\n=== 执行完成 ===")
    print(f"命令: {execution_info['command']}")
    print(f"状态: {'成功' if execution_info['success'] else '失败'}")
    print(f"日志: {execution_info['log_path']}")

    if not execution_info['success']:
        print(f"\n[警告] 执行失败！建议操作:")
        print(f"1. 查看日志: type {execution_info['log_path']}")
        print(f"2. 使用 log-analyze 分析: /log-analyze {execution_info['log_path']}")
        print(f"3. 检查命令语法或环境配置")


if __name__ == '__main__':
    main()