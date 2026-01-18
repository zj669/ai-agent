#!/usr/bin/env python3
"""
HTTP API Client - 功能完整的HTTP请求工具

支持所有HTTP方法(GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS)
支持SSE流式响应处理
支持灵活的请求参数配置
支持终端显示和文件输出两种模式

使用示例:
    python api_client.py --method GET https://api.example.com/users
    python api_client.py --method POST --data '{"name":"张三"}' https://api.example.com/users
    python api_client.py --header "Authorization: Bearer token" https://api.example.com/protected
    python api_client.py --output response.json https://api.example.com/data
    python api_client.py --stream https://api.example.com/events
"""

import argparse
import json
import sys
import os
import urllib.parse
from typing import Dict, List, Optional, Any
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import time

class APIClient:
    def __init__(self, timeout: int = 30, verbose: bool = False):
        self.timeout = timeout
        self.verbose = verbose
        self.session = self._create_session()
        
    def _create_session(self) -> requests.Session:
        """创建带有重试策略的会话"""
        session = requests.Session()
        
        # 配置重试策略
        retry_strategy = Retry(
            total=3,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
        )
        
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        return session
    
    def _print_verbose(self, message: str):
        """详细模式输出"""
        if self.verbose:
            print(f"[VERBOSE] {message}", file=sys.stderr)
    
    def _format_headers(self, headers: Dict[str, str]) -> Dict[str, str]:
        """格式化请求头"""
        formatted = {}
        for header in headers:
            if ':' in header:
                key, value = header.split(':', 1)
                formatted[key.strip()] = value.strip()
            else:
                print(f"警告: 无效的请求头格式 '{header}'，应为 'Key: Value'", file=sys.stderr)
        return formatted
    
    def _format_params(self, params: List[str]) -> Dict[str, str]:
        """格式化查询参数"""
        formatted = {}
        for param in params:
            if '=' in param:
                key, value = param.split('=', 1)
                formatted[key.strip()] = value.strip()
            else:
                print(f"警告: 无效的参数格式 '{param}'，应为 'key=value'", file=sys.stderr)
        return formatted
    
    def _prepare_request_data(self, data: Optional[str], headers: Dict[str, str]) -> tuple:
        """准备请求数据和内容类型"""
        if not data:
            return None, headers
            
        # 检查是否为JSON数据
        try:
            json.loads(data)
            if 'Content-Type' not in headers:
                headers['Content-Type'] = 'application/json'
            return data, headers
        except json.JSONDecodeError:
            # 不是JSON，当作普通文本处理
            if 'Content-Type' not in headers:
                headers['Content-Type'] = 'text/plain'
            return data, headers
    
    def _print_response_summary(self, response: requests.Response):
        """打印响应摘要"""
        status_color = {
            2: '\033[92m',  # 绿色 - 2xx 成功
            3: '\033[93m',  # 黄色 - 3xx 重定向
            4: '\033[91m',  # 红色 - 4xx 客户端错误
            5: '\033[91m',  # 红色 - 5xx 服务器错误
        }.get(response.status_code // 100, '\033[0m')
        
        reset_color = '\033[0m'
        
        print(f"\n{status_color}HTTP/{response.raw.version / 10:.1f} {response.status_code} {response.reason}{reset_color}")
        print(f"响应时间: {response.elapsed.total_seconds():.3f}s")
        print(f"内容长度: {len(response.content)} bytes")
        
        if self.verbose:
            print("\n--- 响应头 ---")
            for key, value in response.headers.items():
                print(f"{key}: {value}")
    
    def _print_formatted_response(self, response: requests.Response):
        """格式化打印响应内容"""
        content_type = response.headers.get('content-type', '').lower()
        
        try:
            if 'application/json' in content_type:
                # JSON格式化输出
                json_data = response.json()
                print("\n--- 响应内容 (JSON) ---")
                print(json.dumps(json_data, indent=2, ensure_ascii=False))
            else:
                # 普通文本输出
                print("\n--- 响应内容 ---")
                print(response.text)
        except json.JSONDecodeError:
            # JSON解析失败，按文本处理
            print("\n--- 响应内容 ---")
            print(response.text)
        except Exception as e:
            print(f"\n--- 响应内容 (解析错误: {e}) ---")
            print(response.text[:1000] + "..." if len(response.text) > 1000 else response.text)
    
    def _save_to_file(self, content: Any, filepath: str):
        """保存内容到文件"""
        # 确保目录存在
        directory = os.path.dirname(filepath)
        if directory and not os.path.exists(directory):
            os.makedirs(directory)
            
        # 保存内容
        if isinstance(content, (dict, list)):
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(content, f, indent=2, ensure_ascii=False)
        else:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(str(content))
                
        print(f"\n✅ 响应已保存到: {filepath}")
    
    def _handle_sse_stream(self, response: requests.Response):
        """处理SSE流式响应"""
        print("📡 开始接收SSE流式响应...")
        print("(按 Ctrl+C 停止)")
        
        try:
            for line in response.iter_lines(decode_unicode=True):
                if line:
                    timestamp = time.strftime('%H:%M:%S')
                    print(f"[{timestamp}] {line}")
        except KeyboardInterrupt:
            print("\n⏹️  流式响应已停止")
        except Exception as e:
            print(f"\n❌ 流式响应错误: {e}")
    
    def execute_request(self, 
                       url: str,
                       method: str = 'GET',
                       headers: Optional[List[str]] = None,
                       params: Optional[List[str]] = None,
                       data: Optional[str] = None,
                       output_file: Optional[str] = None,
                       stream: bool = False) -> bool:
        """
        执行HTTP请求
        
        Args:
            url: 请求URL
            method: HTTP方法
            headers: 请求头列表 ['Key: Value', ...]
            params: 查询参数列表 ['key=value', ...]
            data: 请求体数据
            output_file: 输出文件路径
            stream: 是否启用流式响应
            
        Returns:
            bool: 请求是否成功
        """
        try:
            # 准备请求参数
            req_headers = self._format_headers(headers or [])
            req_params = self._format_params(params or [])
            req_data, req_headers = self._prepare_request_data(data, req_headers)
            
            self._print_verbose(f"请求URL: {url}")
            self._print_verbose(f"请求方法: {method}")
            self._print_verbose(f"请求头: {req_headers}")
            self._print_verbose(f"查询参数: {req_params}")
            if req_data:
                self._print_verbose(f"请求体: {req_data}")
            
            # 发送请求
            response = self.session.request(
                method=method.upper(),
                url=url,
                headers=req_headers,
                params=req_params,
                data=req_data,
                timeout=self.timeout,
                stream=stream
            )
            
            # 处理响应
            self._print_response_summary(response)
            
            if stream and 'text/event-stream' in response.headers.get('content-type', ''):
                self._handle_sse_stream(response)
            else:
                # 标准响应处理
                self._print_formatted_response(response)
                
                # 保存到文件（如果指定了输出路径）
                if output_file:
                    try:
                        # 尝试解析为JSON，失败则保存原始内容
                        content = response.json() if 'application/json' in response.headers.get('content-type', '') else response.text
                        self._save_to_file(content, output_file)
                    except:
                        self._save_to_file(response.text, output_file)
            
            # 检查状态码
            if not (200 <= response.status_code < 300):
                print(f"\n⚠️  警告: HTTP状态码 {response.status_code} 表示请求可能未成功", file=sys.stderr)
                return False
                
            return True
            
        except requests.exceptions.Timeout:
            print(f"❌ 请求超时 ({self.timeout}秒)", file=sys.stderr)
            return False
        except requests.exceptions.ConnectionError as e:
            print(f"❌ 连接错误: {e}", file=sys.stderr)
            return False
        except requests.exceptions.RequestException as e:
            print(f"❌ 请求错误: {e}", file=sys.stderr)
            return False
        except Exception as e:
            print(f"❌ 未知错误: {e}", file=sys.stderr)
            return False

def main():
    parser = argparse.ArgumentParser(
        description='HTTP API客户端 - 支持所有HTTP方法和SSE流式响应',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  %(prog)s --method GET https://api.example.com/users
  %(prog)s --method POST --data '{"name":"张三"}' https://api.example.com/users
  %(prog)s --header "Authorization: Bearer token" https://api.example.com/protected
  %(prog)s --output response.json https://api.example.com/data
  %(prog)s --stream https://api.example.com/events
        """
    )
    
    # 基本参数
    parser.add_argument('url', help='请求URL')
    parser.add_argument('-X', '--method', default='GET', 
                       choices=['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'],
                       help='HTTP方法 (默认: GET)')
    
    # 请求参数
    parser.add_argument('-H', '--header', action='append', default=[],
                       help='请求头，格式: "Key: Value" (可多次使用)')
    parser.add_argument('-p', '--param', action='append', default=[],
                       help='查询参数，格式: "key=value" (可多次使用)')
    parser.add_argument('-d', '--data', help='请求体数据')
    
    # 输出控制
    parser.add_argument('-o', '--output', help='输出文件路径')
    parser.add_argument('-s', '--stream', action='store_true', 
                       help='启用流式响应(SSE)')
    
    # 其他选项
    parser.add_argument('-t', '--timeout', type=int, default=30,
                       help='超时时间(秒，默认: 30)')
    parser.add_argument('-v', '--verbose', action='store_true',
                       help='详细输出模式')
    
    args = parser.parse_args()
    
    # 创建客户端并执行请求
    client = APIClient(timeout=args.timeout, verbose=args.verbose)
    success = client.execute_request(
        url=args.url,
        method=args.method,
        headers=args.header,
        params=args.param,
        data=args.data,
        output_file=args.output,
        stream=args.stream
    )
    
    # 设置退出码
    sys.exit(0 if success else 1)

if __name__ == '__main__':
    main()