package com.zj.aiagent.infrastructure.memory;

import com.zj.aiagent.domain.memory.valobj.Document;
import com.zj.aiagent.domain.memory.valobj.SearchRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NoOpVectorStoreAdapterTest {

    private final NoOpVectorStoreAdapter adapter = new NoOpVectorStoreAdapter();

    @Test
    void shouldReturnEmptyResultsWhenMilvusDisabled() {
        assertEquals(List.of(), adapter.search("hello", 1L, 5));
        assertEquals(List.of(), adapter.similaritySearch(new SearchRequest("hello", 3)));
        assertEquals(List.of(), adapter.searchKnowledgeByDataset("dataset-1", "hello", 2));
    }

    @Test
    void shouldIgnoreWriteOperationsWhenMilvusDisabled() {
        assertDoesNotThrow(() -> adapter.store(1L, "content", Map.of("k", "v")));
        assertDoesNotThrow(() -> adapter.storeBatch(1L, List.of("a", "b")));
        assertDoesNotThrow(() -> adapter.addDocuments(List.of(new Document("content"))));
        assertDoesNotThrow(() -> adapter.deleteByMetadata(Map.of("documentId", "doc-1")));
    }
}
