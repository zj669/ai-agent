#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
命令执行缓存管理器
专为命令执行结果设计的LRU缓存

核心功能：
1. LRU缓存管理（最近最少使用）
2. 执行结果缓存和快速检索
3. 智能过期策略
4. 缓存统计和清理
"""

import json
import time
import hashlib
from pathlib import Path
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from datetime import datetime


@dataclass
class ExecutionCacheEntry:
    """执行缓存条目"""
    command_hash: str
    command: str
    execution_info: Dict
    timestamp: float
    access_count: int
    success: bool
    feature: str
    log_path: str


class ExecutionLRUCache:
    """执行LRU缓存实现"""

    def __init__(self, max_size: int = 100):
        self.max_size = max_size
        self.cache: Dict[str, ExecutionCacheEntry] = {}
        self.access_order: List[str] = []  # 访问顺序，最新的在末尾

    def _update_access(self, key: str):
        """更新访问记录"""
        if key in self.access_order:
            self.access_order.remove(key)
        self.access_order.append(key)
        if key in self.cache:
            self.cache[key].access_count += 1
            self.cache[key].timestamp = time.time()

    def get(self, key: str) -> Optional[ExecutionCacheEntry]:
        """获取缓存项"""
        if key in self.cache:
            self._update_access(key)
            return self.cache[key]
        return None

    def put(self, key: str, command: str, execution_info: Dict, feature: str = "default") -> bool:
        """放入执行缓存项"""
        # 如果已存在，先删除旧的
        if key in self.cache:
            del self.cache[key]
            self.access_order.remove(key)

        # 检查容量限制
        if len(self.cache) >= self.max_size:
            self._evict_lru()

        # 创建新条目
        entry = ExecutionCacheEntry(
            command_hash=key,
            command=command,
            execution_info=execution_info,
            timestamp=time.time(),
            access_count=1,
            success=execution_info.get("success", False),
            feature=feature,
            log_path=execution_info.get("log_path", "")
        )

        self.cache[key] = entry
        self.access_order.append(key)
        return True

    def _evict_lru(self):
        """淘汰最久未使用的条目"""
        if self.access_order:
            lru_key = self.access_order.pop(0)
            del self.cache[lru_key]

    def get_recent(self, limit: int = 10) -> List[ExecutionCacheEntry]:
        """获取最近的执行记录"""
        recent_keys = self.access_order[-limit:] if len(self.access_order) >= limit else self.access_order
        return [self.cache[key] for key in recent_keys if key in self.cache]

    def get_failed(self, limit: int = 5) -> List[ExecutionCacheEntry]:
        """获取失败的执行记录"""
        failed = [entry for entry in self.cache.values() if not entry.success]
        failed.sort(key=lambda x: x.timestamp, reverse=True)
        return failed[:limit] if len(failed) >= limit else failed

    def get_by_feature(self, feature: str) -> List[ExecutionCacheEntry]:
        """按功能获取执行记录"""
        return [entry for entry in self.cache.values() if entry.feature == feature]

    def cleanup_expired(self, ttl_hours: int = 24):
        """清理过期条目"""
        cutoff_time = time.time() - (ttl_hours * 3600)
        expired_keys = [
            key for key, entry in self.cache.items()
            if entry.timestamp < cutoff_time
        ]

        for key in expired_keys:
            del self.cache[key]
            if key in self.access_order:
                self.access_order.remove(key)

    def get_stats(self) -> Dict:
        """获取缓存统计信息"""
        total_access = sum(entry.access_count for entry in self.cache.values())
        if len(self.cache) == 0:
            hit_rate = 0.0
        else:
            hits = sum(1 for entry in self.cache.values() if entry.access_count > 1)
            hit_rate = round(hits / len(self.cache), 3)

        return {
            "size": len(self.cache),
            "max_size": self.max_size,
            "total_access": total_access,
            "hit_rate": hit_rate,
            "failed_count": sum(1 for entry in self.cache.values() if not entry.success),
            "success_count": sum(1 for entry in self.cache.values() if entry.success)
        }


class ExecutionCacheManager:
    """执行缓存管理器"""

    def __init__(self, cache_dir: str = ".business/cache"):
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        self.memory_cache = ExecutionLRUCache(max_size=100)

    def save_execution(self, execution_info: Dict):
        """保存执行信息到持久化存储"""
        try:
            cache_file = self.cache_dir / "executions.json"

            if cache_file.exists():
                with open(cache_file, 'r', encoding='utf-8') as f:
                    cache = json.load(f)
            else:
                cache = []

            # 添加新记录
            cache.append(execution_info)

            # 保持最近200条记录
            if len(cache) > 200:
                cache = cache[-200:]

            with open(cache_file, 'w', encoding='utf-8') as f:
                json.dump(cache, f, indent=2, ensure_ascii=False)

            # 同时更新内存缓存
            command_hash = hashlib.md5(execution_info["command"].encode()).hexdigest()
            self.memory_cache.put(
                key=command_hash,
                command=execution_info["command"],
                execution_info=execution_info,
                feature=execution_info.get("feature", "default")
            )

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
    """测试缓存管理器"""
    import argparse

    parser = argparse.ArgumentParser(description='执行缓存管理器测试工具')
    parser.add_argument('--test-memory', action='store_true', help='测试内存缓存')
    parser.add_argument('--test-disk', action='store_true', help='测试磁盘缓存')
    parser.add_argument('--stats', action='store_true', help='显示缓存统计')

    args = parser.parse_args()

    if args.test_memory:
        print("=== 内存缓存测试 ===")
        cache = ExecutionLRUCache(max_size=5)

        # 添加测试数据
        for i in range(7):
            cache.put(
                key=f"hash_{i}",
                command=f"mvn test {i}",
                execution_info={
                    "command": f"mvn test {i}",
                    "success": i % 2 == 0,
                    "log_path": f"/logs/test_{i}.log",
                    "exit_code": 0 if i % 2 == 0 else 1
                },
                feature=f"feature_{i % 3}"
            )
            print(f"添加命令 {i}, 缓存大小: {len(cache.cache)}")

        # 访问某些键
        cache.get("hash_2")
        cache.get("hash_4")

        print(f"最终缓存键: {list(cache.cache.keys())}")
        print(f"访问顺序: {cache.access_order}")

        # 测试查询功能
        print(f"\n最近的3个记录:")
        for entry in cache.get_recent(3):
            print(f"  {entry.command} ({'成功' if entry.success else '失败'})")

        print(f"\n失败的记录:")
        for entry in cache.get_failed():
            print(f"  {entry.command}")

    elif args.test_disk:
        print("=== 磁盘缓存测试 ===")
        manager = ExecutionCacheManager()

        # 保存一些测试数据
        for i in range(5):
            execution_info = {
                "command": f"test_command_{i}",
                "success": i % 2 == 0,
                "exit_code": 0 if i % 2 == 0 else 1,
                "log_path": f"/path/to/log_{i}.log",
                "timestamp": datetime.now().isoformat(),
                "feature": f"test_feature"
            }
            manager.save_execution(execution_info)
            print(f"保存执行记录 {i}")

        # 读取记录
        print(f"\n最近的记录:")
        for record in manager.get_recent_executions(5):
            print(f"  {record['command']} ({'成功' if record['success'] else '失败'})")

    elif args.stats:
        print("=== 缓存统计 ===")
        manager = ExecutionCacheManager()
        recent = manager.get_recent_executions()
        failed = manager.get_failed_executions()

        print(f"总记录数: {len(recent)}")
        print(f"失败记录数: {len(failed)}")
        print(f"成功率: {((len(recent) - len(failed)) / len(recent) * 100 if recent else 0):.1f}%")

    else:
        parser.print_help()


if __name__ == '__main__':
    main()