#!/usr/bin/env python3
"""
知识库上传+检索冒烟脚本

用法：
  python scripts/knowledge_upload_retrieval_smoke.py

可选环境变量：
  BASE_URL=http://localhost:8080
  EMAIL=3218356902zj@gmail.com
  PASSWORD=18090932802zj
  DEBUG_USER=1   # 登录失败时可走 debug-user 头（application-local 默认开启）
  KEEP_DATASET=1 # 不自动删除测试知识库
"""

from __future__ import annotations

import json
import os
import sys
import tempfile
import time
from pathlib import Path

import requests

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")
EMAIL = os.getenv("EMAIL", "3218356902zj@gmail.com")
PASSWORD = os.getenv("PASSWORD", "18090932802zj")
DEBUG_USER = os.getenv("DEBUG_USER", "1")
KEEP_DATASET = os.getenv("KEEP_DATASET", "0") == "1"


class ApiError(RuntimeError):
    pass


def _safe(msg: str):
    try:
        print(msg, flush=True)
    except UnicodeEncodeError:
        # Windows 默认控制台可能是 GBK，回退为可编码文本，避免掩盖原始异常
        fallback = msg.encode("ascii", errors="replace").decode("ascii")
        print(fallback, flush=True)


def _unwrap(resp: requests.Response):
    try:
        data = resp.json()
    except Exception:
        raise ApiError(f"HTTP {resp.status_code} 非JSON响应: {resp.text[:300]}")

    if resp.status_code != 200:
        raise ApiError(f"HTTP {resp.status_code}: {json.dumps(data, ensure_ascii=False)[:500]}")

    code = data.get("code")
    if code != 200:
        raise ApiError(f"业务错误 code={code}: {json.dumps(data, ensure_ascii=False)[:500]}")
    return data.get("data")


def login(session: requests.Session) -> dict:
    url = f"{BASE_URL}/api/user/login"
    payload = {"email": EMAIL, "password": PASSWORD}
    _safe(f"[1/6] 登录: {EMAIL}")

    headers = {}
    try:
        resp = session.post(url, json=payload, timeout=20)
        data = _unwrap(resp)
        token = None
        if isinstance(data, dict):
            token = data.get("accessToken") or data.get("token")

        if token:
            headers["Authorization"] = f"Bearer {token}"
            _safe("  - 登录成功（JWT）")
        else:
            _safe("  - 登录返回无 token，尝试 debug-user 头")
    except Exception as e:
        _safe(f"  - 登录失败，回退 debug-user 头: {e}")

    if DEBUG_USER:
        headers["debug-user"] = str(DEBUG_USER)

    return headers


def create_dataset(session: requests.Session, headers: dict) -> str:
    url = f"{BASE_URL}/api/knowledge/dataset"
    name = f"kb-smoke-{int(time.time())}"
    payload = {
        "name": name,
        "description": "自动化冒烟测试：上传+检索"
    }
    _safe("[2/6] 创建测试知识库")
    resp = session.post(url, json=payload, headers=headers, timeout=20)
    data = _unwrap(resp)
    dataset_id = data["datasetId"]
    _safe(f"  - datasetId={dataset_id}")
    return dataset_id


def upload_document(session: requests.Session, headers: dict, dataset_id: str) -> str:
    _safe("[3/6] 上传测试文档")
    url = f"{BASE_URL}/api/knowledge/document/upload"

    unique_fact = f"ProjectCodename=ABU-KB-SMOKE-{int(time.time())}"
    content = (
        "这是知识库检索冒烟测试文档。\n"
        "如果检索链路正常，应能搜到下列唯一字段：\n"
        f"{unique_fact}\n"
        "结论：知识库上传、分块、向量化、检索链路可用。\n"
    )

    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as fp:
        fp.write(content)
        tmp_path = Path(fp.name)

    files = {
        "file": (tmp_path.name, tmp_path.read_bytes(), "text/plain")
    }
    data = {
        "datasetId": dataset_id,
        "chunkSize": "300",
        "chunkOverlap": "30",
    }

    try:
        resp = session.post(url, files=files, data=data, headers=headers, timeout=60)
        doc = _unwrap(resp)
        document_id = doc["documentId"]
        _safe(f"  - documentId={document_id}")
        return document_id, unique_fact
    finally:
        try:
            tmp_path.unlink(missing_ok=True)
        except Exception:
            pass


def wait_document_complete(session: requests.Session, headers: dict, dataset_id: str, document_id: str):
    _safe("[4/6] 轮询文档处理状态")
    url = f"{BASE_URL}/api/knowledge/document/list"
    deadline = time.time() + 180
    status = ""

    while time.time() < deadline:
        resp = session.get(url, params={"datasetId": dataset_id, "page": 0, "size": 50}, headers=headers, timeout=20)
        page = _unwrap(resp)
        content = page.get("content", []) if isinstance(page, dict) else []
        target = next((d for d in content if d.get("documentId") == document_id), None)

        if not target:
            _safe("  - 警告：列表中暂未看到文档，继续等待...")
            time.sleep(2)
            continue

        status = target.get("status") or ""
        processed = target.get("processedChunks")
        total = target.get("totalChunks")
        _safe(f"  - status={status}, chunks={processed}/{total}")

        if status == "COMPLETED":
            return
        if status == "FAILED":
            raise ApiError(f"文档处理失败: {target.get('errorMessage')}")

        time.sleep(2)

    raise ApiError(f"文档处理超时，最后状态={status}")


def search(session: requests.Session, headers: dict, dataset_id: str, query: str):
    _safe("[5/6] 执行检索")
    url = f"{BASE_URL}/api/knowledge/search"
    payload = {
        "datasetId": dataset_id,
        "query": query,
        "topK": 3,
    }
    resp = session.post(url, json=payload, headers=headers, timeout=30)
    data = _unwrap(resp)
    if not isinstance(data, list):
        raise ApiError(f"检索返回格式异常: {type(data)}")

    _safe(f"  - 返回 {len(data)} 条")
    for idx, item in enumerate(data, 1):
        preview = (item or "")[:120].replace("\n", " ")
        _safe(f"    {idx}. {preview}")
    return data


def cleanup(session: requests.Session, headers: dict, dataset_id: str):
    if KEEP_DATASET:
        _safe("[6/6] 跳过清理（KEEP_DATASET=1）")
        return

    _safe("[6/6] 清理测试知识库")
    url = f"{BASE_URL}/api/knowledge/dataset/{dataset_id}"
    resp = session.delete(url, headers=headers, timeout=30)
    _unwrap(resp)
    _safe("  - 清理完成")


def main():
    session = requests.Session()
    headers = login(session)

    dataset_id = None
    try:
        dataset_id = create_dataset(session, headers)
        document_id, unique_fact = upload_document(session, headers, dataset_id)
        wait_document_complete(session, headers, dataset_id, document_id)

        results = search(session, headers, dataset_id, unique_fact)
        ok = any(unique_fact in (r or "") for r in results)

        if not ok:
            raise ApiError("检索未命中唯一测试字段，可能存在 embedding/向量检索问题")

        _safe("[PASS] 冒烟通过：上传+向量化+检索链路正常")
        return 0
    except Exception as e:
        _safe(f"❌ 冒烟失败：{e}")
        return 1
    finally:
        if dataset_id:
            try:
                cleanup(session, headers, dataset_id)
            except Exception as e:
                _safe(f"⚠️ 清理失败（可手动删除 datasetId={dataset_id}）：{e}")


if __name__ == "__main__":
    sys.exit(main())
