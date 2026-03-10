# 知识库链路修复提交方案（二选一）

基于最小补丁：`docs/bugfix-log/knowledge-chain-fix-minimal.patch`

## 方案 A：挑拣提交（在当前分支）
适合：想最快把修复落到当前分支。

```bash
git add ai-agent-interfaces/src/main/java/com/zj/aiagent/config/EmbeddingModelConfig.java
git add ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java
git commit -m "fix(knowledge): repair embedding endpoint path and auto-init Milvus schema"
```

## 方案 B：新分支隔离提交（更干净）
适合：当前分支脏改动太多，想要最干净评审。

```bash
git switch -c fix/knowledge-chain-recovery
# 仅拣入最小补丁
git apply --index docs/bugfix-log/knowledge-chain-fix-minimal.patch
git commit -m "fix(knowledge): repair embedding endpoint path and auto-init Milvus schema"
```

## 验收结论（已通过）
- 冒烟脚本：`scripts/knowledge_upload_retrieval_smoke.py`
- 结果：上传 -> 切块 -> 检索全链路 PASS
