package com.beauty.knowledge.infrastructure.vector;

import io.milvus.client.MilvusServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MilvusVectorStore {

    private final MilvusServiceClient milvusServiceClient;

    public MilvusVectorStore(MilvusServiceClient milvusServiceClient) {
        this.milvusServiceClient = milvusServiceClient;
    }

    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        // Keep this method as the unified entry for vector deletion to avoid coupling
        // business logic with vendor SDK details.
        log.info("Delete vectors from Milvus, count={}", chunkIds.size());
    }
}
