# VectorStore deleteByMetadata 修复设计文档

## 1. 问题描述

**现状**：`MilvusVectorStoreAdapter.deleteByMetadata()` 方法抛出 `UnsupportedOperationException`

```java
// 当前实现 (line 225-237)
@Override
public void deleteByMetadata(Map<String, Object> filter) {
    throw new UnsupportedOperationException(
            "Delete by metadata not directly supported. Use search + delete by ID instead.");
}
```

**影响范围**：
- `AsyncDocumentProcessor.deleteDocumentVectors()` 调用此方法删除文档向量
- 删除知识库文档时无法正确清理 Milvus 中的向量数据

---

## 2. 技术方案

### 方案选择：Search + Delete by ID

Spring AI 的 `VectorStore` 接口支持 `delete(List<String> ids)` 方法，因此采用两步删除策略：

1. **Step 1**: 使用 `filterExpression` 检索匹配的 Document IDs
2. **Step 2**: 调用 `delete(ids)` 批量删除

### 实现逻辑

```java
@Override
public void deleteByMetadata(Map<String, Object> filter) {
    // 1. 构建 Filter 表达式
    String filterExpression = buildFilterExpression(filter);
    
    // 2. 检索所有匹配的文档 (需要获取足够多)
    SearchRequest request = SearchRequest.builder()
            .query("")  // 空查询
            .topK(1000) // 足够大的 topK
            .filterExpression(filterExpression)
            .build();
    
    List<Document> results = knowledgeStore.similaritySearch(request);
    
    // 3. 提取 Document IDs
    List<String> ids = results.stream()
            .map(Document::getId)
            .collect(Collectors.toList());
    
    // 4. 批量删除
    if (!ids.isEmpty()) {
        knowledgeStore.delete(ids);
    }
}
```

---

## 3. Proposed Changes

### [MODIFY] [MilvusVectorStoreAdapter.java](file:///D:/java/ai-agent/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java)

1. 实现 `deleteByMetadata()` 方法（替换当前的 `UnsupportedOperationException`）
2. 添加辅助方法 `buildFilterExpression(Map<String, Object> filter)`

---

## 4. Verification Plan

### 4.1 编译验证
```bash
mvn compile -DskipTests -q
```

### 4.2 单元测试（可选）
由于 Milvus 需要实际连接，建议在本地启用 `MILVUS_ENABLED=true` 后手动测试：

1. 上传一个测试文档到知识库
2. 通过 API 删除该文档
3. 验证 Milvus Collection 中对应的向量已被删除

---

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 空查询可能不返回结果 | 中 | 使用通用查询词或跳过相似度阈值 |
| topK=1000 可能不够 | 低 | 可分页处理或增大 topK |
| Milvus 连接失败 | 中 | 捕获异常并记录日志 |

---

> **⛔ STOP POINT**: 设计文档已生成。请审核。（输入'通过'进入编码阶段）
