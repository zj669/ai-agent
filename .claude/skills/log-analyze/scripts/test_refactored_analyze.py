#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试重构后的analyze.py功能
"""

import tempfile
import os
from pathlib import Path
from analyze import LogAnalyzer

def test_error_type_identification():
    """测试错误类型识别功能"""
    print("=== 测试错误类型识别 ===")
    
    analyzer = LogAnalyzer("dummy.log")
    
    # 测试各种错误类型
    test_cases = [
        ("[ERROR] cannot find symbol: SomeClass", "COMPILATION_SYMBOL_ERROR"),
        ("[ERROR] package com.example does not exist", "COMPILATION_PACKAGE_ERROR"),
        ("[ERROR] compilation failure", "COMPILATION_GENERAL_FAILURE"),
        ("[ERROR] incompatible types", "COMPILATION_TYPE_ERROR"),
        ("[ERROR] ; expected", "COMPILATION_SYNTAX_ERROR"),
        ("[ERROR] java.lang.NullPointerException", "RUNTIME_NULL_POINTER"),
        ("[ERROR] java.lang.ClassCastException", "RUNTIME_CLASS_CAST"),
        ("[ERROR] DependencyResolutionException", "BUILD_DEPENDENCY_ERROR"),
        ("[ERROR] Failed to execute goal", "BUILD_PLUGIN_FAILURE"),
        ("[ERROR] Unknown error occurred", "GENERIC_ERROR")
    ]
    
    for error_content, expected_type in test_cases:
        actual_type = analyzer._identify_error_type_from_content(error_content)
        status = "✅" if actual_type == expected_type else "❌"
        print(f"{status} {error_content[:30]:30} -> {actual_type} (期望: {expected_type})")

def test_greedy_stack_extraction():
    """测试贪婪堆栈提取"""
    print("\n=== 测试贪婪堆栈提取 ===")
    
    # 创建测试日志内容
    test_log_content = """[ERROR] java.lang.IllegalStateException: Failed to load ApplicationContext
Discovered dependency mechanism implies implementations...
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:583)
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:732)
Caused by: java.lang.IllegalArgumentException: Invalid configuration
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:333)
[INFO] Building project version 1.0.0
"""
    
    with tempfile.NamedTemporaryFile(mode='w', suffix='.log', delete=False) as f:
        f.write(test_log_content)
        temp_log_path = f.name
    
    try:
        analyzer = LogAnalyzer(temp_log_path)
        lines = analyzer.read_lines()
        
        # 测试堆栈提取
        stack_trace, next_pos = analyzer._extract_greedy_stack_trace(lines, 0)
        
        print(f"提取的堆栈行数: {len(stack_trace)}")
        print("堆栈内容:")
        for i, line in enumerate(stack_trace):
            print(f"  {i+1:2d}: {line}")
        
        print(f"\n下一个处理位置: {next_pos}")
        print(f"终止信号检测: {analyzer._is_termination_signal('[INFO] Building project', 4, 0)}")
        
    finally:
        os.unlink(temp_log_path)

def test_complete_analysis():
    """测试完整分析流程"""
    print("\n=== 测试完整分析流程 ===")
    
    # 创建包含多个错误的测试日志
    test_log_content = """[INFO] Starting build process
[ERROR] cannot find symbol: UserService
at com.example.service.UserServiceImpl.java:45:12
[ERROR] java.lang.NullPointerException
at com.example.controller.UserController.java:123:8
Caused by: java.lang.IllegalStateException
[INFO] Build completed with errors
"""
    
    with tempfile.NamedTemporaryFile(mode='w', suffix='.log', delete=False) as f:
        f.write(test_log_content)
        temp_log_path = f.name
    
    try:
        analyzer = LogAnalyzer(temp_log_path)
        errors = analyzer.analyze()
        
        print(f"发现错误数量: {len(errors)}")
        
        for i, error in enumerate(errors, 1):
            print(f"\n错误 #{i}:")
            print(f"  类型: {error.error_type}")
            print(f"  位置: 第 {error.line_number} 行")
            print(f"  文件: {error.file_name}")
            if error.line_num:
                print(f"  行号: {error.line_num}")
            print(f"  堆栈行数: {len(error.stack_trace)}")
            
        # 测试完整报告生成
        report = analyzer.generate_complete_report()
        print(f"\n=== 生成的报告预览 ===")
        print(report[:500] + "..." if len(report) > 500 else report)
        
    finally:
        os.unlink(temp_log_path)

if __name__ == '__main__':
    print("开始测试重构后的analyze.py功能...\n")
    
    test_error_type_identification()
    test_greedy_stack_extraction()
    test_complete_analysis()
    
    print("\n✅ 所有测试完成!")