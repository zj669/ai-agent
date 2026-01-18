#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
缓存管理器
实现LRU缓存、增量分析和智能过期机制

核心功能：
1. LRU缓存管理（最近最少使用）
2. 增量日志分析
3. 智能过期策略
4. 缓存统计和清理
"""

import json
import time
import os
import hashlib
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta


@dataclass
class CacheEntry:
    """缓存条目"""
    key: str
    data: Any
    timestamp: float
    access_count: int
    size: int  # 字节大小
    hash: str  # 内容哈希


class LRUCache:
    """LRU缓存实现"""
    
    def __init__(self, max_size: int = 100, max_memory_mb: int = 50):
        self.max_size = max_size
        self.max_memory_bytes = max_memory_mb * 1024 * 1024
        self.cache: Dict[str, CacheEntry] = {}
        self.access_order: List[str] = []  # 访问顺序，最新的在末尾
        self.current_memory = 0
        
    def _update_access(self, key: str):
        """更新访问记录"""
        if key in self.access_order:
            self.access_order.remove(key)
        self.access_order.append(key)
        self.cache[key].access_count += 1
        self.cache[key].timestamp = time.time()
    
    def get(self, key: str) -> Optional[Any]:
        """获取缓存项"""
        if key in self.cache:
            self._update_access(key)
            return self.cache[key].data
        return None
    
    def put(self, key: str, data: Any, size: Optional[int] = None) -> bool:
        """放入缓存项"""
        # 计算大小
        if size is None:
            size = len(str(data).encode('utf-8'))
        
        # 如果已存在，先删除旧的
        if key in self.cache:
            old_size = self.cache[key].size
            del self.cache[key]
            self.access_order.remove(key)
            self.current_memory -= old_size
        
        # 检查容量限制
        if not self._check_capacity(size):
            return False
        
        # 创建新条目
        entry = CacheEntry(
            key=key,
            data=data,
            timestamp=time.time(),
            access_count=1,
            size=size,
            hash=hashlib.md5(str(data).encode()).hexdigest()
        )
        
        self.cache[key] = entry
        self.access_order.append(key)
        self.current_memory += size
        return True
    
    def _check_capacity(self, new_size: int) -> bool:
        """检查是否超出容量限制"""
        # 检查内存限制
        if self.current_memory + new_size > self.max_memory_bytes:
            self._evict_by_memory(new_size)
        
        # 检查数量限制
        if len(self.cache) >= self.max_size:
            self._evict_lru()
        
        return True
    
    def _evict_lru(self):
        """淘汰最久未使用的条目"""
        if self.access_order:
            lru_key = self.access_order.pop(0)
            entry = self.cache.pop(lru_key)
            self.current_memory -= entry.size
    
    def _evict_by_memory(self, required_space: int):
        """根据内存需求淘汰条目"""
        # 按访问次数排序，优先淘汰访问次数少的
        sorted_entries = sorted(
            self.cache.values(), 
            key=lambda x: (x.access_count, x.timestamp)
        )
        
        freed_space = 0
        for entry in sorted_entries:
            if freed_space >= required_space:
                break
            self.cache.pop(entry.key)
            self.access_order.remove(entry.key)
            self.current_memory -= entry.size
            freed_space += entry.size
    
    def cleanup_expired(self, ttl_hours: int = 24):
        """清理过期条目"""
        cutoff_time = time.time() - (ttl_hours * 3600)
        expired_keys = [
            key for key, entry in self.cache.items() 
            if entry.timestamp < cutoff_time
        ]
        
        for key in expired_keys:
            entry = self.cache.pop(key)
            self.access_order.remove(key)
            self.current_memory -= entry.size
    
    def get_stats(self) -> Dict:
        """获取缓存统计信息"""
        return {
            "size": len(self.cache),
            "max_size": self.max_size,
            "memory_usage_mb": round(self.current_memory / (1024 * 1024), 2),
            "max_memory_mb": self.max_memory_bytes / (1024 * 1024),
            "hit_rate": self._calculate_hit_rate()
        }
    
    def _calculate_hit_rate(self) -> float:
        """计算命中率"""
        total_access = sum(entry.access_count for entry in self.cache.values())
        if total_access == 0:
            return 0.0
        hits = sum(1 for entry in self.cache.values() if entry.access_count > 1)
        return round(hits / len(self.cache), 3)


class IncrementalAnalyzer:
    """增量分析器"""
    
    def __init__(self, cache: LRUCache):
        self.cache = cache
        self.last_positions: Dict[str, int] = {}  # 文件路径 -> 最后分析位置
    
    def analyze_incremental(self, log_path: str, analyzer_func) -> Dict:
        """
        增量分析日志文件
        
        Args:
            log_path: 日志文件路径
            analyzer_func: 分析函数
            
        Returns:
            dict: 增量分析结果
        """
        log_file = Path(log_path)
        if not log_file.exists():
            return {"error": "日志文件不存在"}
        
        current_size = log_file.stat().st_size
        last_position = self.last_positions.get(log_path, 0)
        
        # 如果文件变小了（可能被清空），重新开始
        if current_size < last_position:
            last_position = 0
            self.last_positions[log_path] = 0
        
        # 如果没有新内容，直接返回缓存结果
        if current_size <= last_position:
            cached_result = self.cache.get(f"incremental_{log_path}")
            if cached_result:
                return cached_result
            return {"message": "无新增内容"}
        
        # 读取新增内容
        try:
            with open(log_path, 'rb') as f:
                f.seek(last_position)
                new_content = f.read()
            
            # 更新位置记录
            self.last_positions[log_path] = current_size
            
            # 分析新增内容
            result = analyzer_func(new_content)
            
            # 缓存结果
            cache_key = f"incremental_{log_path}"
            self.cache.put(cache_key, result, len(str(result)))
            
            return {
                "new_content_size": len(new_content),
                "analysis_result": result,
                "last_position": current_size
            }
            
        except Exception as e:
            return {"error": f"增量分析失败: {e}"}


class LogManager:
    """日志管理器"""
    
    def __init__(self, base_dir: str = ".business"):
        self.base_dir = Path(base_dir)
        self.cache = LRUCache(max_size=50, max_memory_mb=30)
        self.incremental_analyzer = IncrementalAnalyzer(self.cache)
        
    def get_log_files(self, feature: str = None) -> List[Dict]:
        """获取日志文件列表"""
        log_files = []
        
        search_path = self.base_dir
        if feature:
            search_path = self.base_dir / feature / "executelogs"
        
        if search_path.exists():
            for log_file in search_path.rglob("*.log"):
                stat = log_file.stat()
                log_files.append({
                    "path": str(log_file),
                    "size": stat.st_size,
                    "modified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
                    "name": log_file.name
                })
        
        # 按修改时间排序
        return sorted(log_files, key=lambda x: x["modified"], reverse=True)
    
    def analyze_log_with_cache(self, log_path: str, force_refresh: bool = False) -> Dict:
        """带缓存的日志分析"""
        cache_key = f"log_analysis_{log_path}"
        
        # 检查缓存（除非强制刷新）
        if not force_refresh:
            cached_result = self.cache.get(cache_key)
            if cached_result:
                return {
                    "source": "cache",
                    "result": cached_result
                }
        
        # 执行实际分析
        try:
            from analyze import LogAnalyzer
            analyzer = LogAnalyzer(log_path)
            errors = analyzer.analyze()
            
            result = {
                "error_count": len(errors),
                "errors": [asdict(err) for err in errors[:3]],  # 只缓存前3个错误详情
                "summary": "发现错误" if errors else "无错误",
                "timestamp": time.time()
            }
            
            # 缓存结果（1小时过期）
            self.cache.put(cache_key, result, len(str(result)))
            
            return {
                "source": "fresh",
                "result": result
            }
            
        except Exception as e:
            return {
                "source": "error",
                "error": str(e)
            }
    
    def get_cache_stats(self) -> Dict:
        """获取缓存统计"""
        return self.cache.get_stats()
    
    def cleanup_cache(self):
        """清理缓存"""
        self.cache.cleanup_expired(ttl_hours=12)
        return self.get_cache_stats()


def main():
    """测试缓存管理器功能"""
    import argparse
    
    parser = argparse.ArgumentParser(description='缓存管理器测试工具')
    parser.add_argument('--test-cache', action='store_true', help='测试LRU缓存')
    parser.add_argument('--test-log', help='测试日志分析缓存')
    parser.add_argument('--stats', action='store_true', help='显示缓存统计')
    parser.add_argument('--cleanup', action='store_true', help='清理过期缓存')
    
    args = parser.parse_args()
    
    manager = LogManager()
    
    if args.test_cache:
        print("=== LRU缓存测试 ===")
        cache = LRUCache(max_size=5)
        
        # 添加测试数据
        for i in range(7):
            cache.put(f"key_{i}", f"value_{i}", 100)
            print(f"添加 key_{i}, 缓存大小: {len(cache.cache)}")
        
        # 访问某些键
        cache.get("key_2")
        cache.get("key_4")
        
        print(f"最终缓存: {list(cache.cache.keys())}")
        print(f"访问顺序: {cache.access_order}")
    
    elif args.test_log:
        print(f"=== 日志分析缓存测试: {args.test_log} ===")
        result = manager.analyze_log_with_cache(args.test_log)
        print(f"来源: {result['source']}")
        if 'result' in result:
            print(f"错误数: {result['result']['error_count']}")
            print(f"摘要: {result['result']['summary']}")
    
    elif args.stats:
        stats = manager.get_cache_stats()
        print("=== 缓存统计 ===")
        for key, value in stats.items():
            print(f"{key}: {value}")
    
    elif args.cleanup:
        print("=== 清理缓存 ===")
        stats_before = manager.get_cache_stats()
        print(f"清理前: {stats_before}")
        
        manager.cleanup_cache()
        
        stats_after = manager.get_cache_stats()
        print(f"清理后: {stats_after}")
    
    else:
        parser.print_help()


if __name__ == '__main__':
    main()