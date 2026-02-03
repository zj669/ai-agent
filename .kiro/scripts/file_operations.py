#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
工作区外文件操作工具
用于可靠地读取和写入 UTF-8 编码的文件，特别是包含中文的文件
"""

import sys
import os
from pathlib import Path


def read_file(file_path):
    """
    读取文件内容（UTF-8 编码）
    
    Args:
        file_path: 文件路径（绝对路径或相对路径）
    
    Returns:
        文件内容字符串，如果失败则返回 None
    """
    try:
        path = Path(file_path).expanduser()
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        print(content)
        return 0
    except FileNotFoundError:
        print(f"错误: 文件不存在 - {file_path}", file=sys.stderr)
        return 1
    except PermissionError:
        print(f"错误: 没有权限读取文件 - {file_path}", file=sys.stderr)
        return 1
    except Exception as e:
        print(f"错误: 读取文件失败 - {e}", file=sys.stderr)
        return 1


def write_file(file_path, content):
    """
    写入文件内容（UTF-8 编码）
    
    Args:
        file_path: 文件路径（绝对路径或相对路径）
        content: 要写入的内容
    
    Returns:
        0 表示成功，1 表示失败
    """
    try:
        path = Path(file_path).expanduser()
        # 确保父目录存在
        path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
        print(f"成功: 文件已写入 - {file_path}")
        return 0
    except PermissionError:
        print(f"错误: 没有权限写入文件 - {file_path}", file=sys.stderr)
        return 1
    except Exception as e:
        print(f"错误: 写入文件失败 - {e}", file=sys.stderr)
        return 1


def append_file(file_path, content):
    """
    追加内容到文件（UTF-8 编码）
    
    Args:
        file_path: 文件路径（绝对路径或相对路径）
        content: 要追加的内容
    
    Returns:
        0 表示成功，1 表示失败
    """
    try:
        path = Path(file_path).expanduser()
        # 确保父目录存在
        path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(path, 'a', encoding='utf-8', newline='\n') as f:
            f.write(content)
        print(f"成功: 内容已追加到文件 - {file_path}")
        return 0
    except PermissionError:
        print(f"错误: 没有权限写入文件 - {file_path}", file=sys.stderr)
        return 1
    except Exception as e:
        print(f"错误: 追加文件失败 - {e}", file=sys.stderr)
        return 1


def main():
    """命令行入口"""
    if len(sys.argv) < 3:
        print("用法:")
        print("  读取文件: python file_operations.py read <文件路径>")
        print("  写入文件: python file_operations.py write <文件路径> <内容>")
        print("  追加文件: python file_operations.py append <文件路径> <内容>")
        print()
        print("示例:")
        print('  python file_operations.py read "C:/Users/xxx/.kiro/steering/test.md"')
        print('  python file_operations.py write "C:/Users/xxx/.kiro/steering/test.md" "中文内容"')
        return 1
    
    operation = sys.argv[1].lower()
    file_path = sys.argv[2]
    
    if operation == 'read':
        return read_file(file_path)
    elif operation == 'write':
        if len(sys.argv) < 4:
            print("错误: write 操作需要提供内容参数", file=sys.stderr)
            return 1
        content = sys.argv[3]
        return write_file(file_path, content)
    elif operation == 'append':
        if len(sys.argv) < 4:
            print("错误: append 操作需要提供内容参数", file=sys.stderr)
            return 1
        content = sys.argv[3]
        return append_file(file_path, content)
    else:
        print(f"错误: 未知操作 '{operation}'", file=sys.stderr)
        print("支持的操作: read, write, append", file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
