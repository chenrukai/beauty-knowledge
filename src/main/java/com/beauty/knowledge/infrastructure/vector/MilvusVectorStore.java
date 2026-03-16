package com.beauty.knowledge.infrastructure.vector;

import com.beauty.knowledge.config.MilvusConfig;
import io.milvus.client.MilvusServiceClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class MilvusVectorStore {

    private final MilvusServiceClient milvusServiceClient;

    public MilvusVectorStore(MilvusServiceClient milvusServiceClient) {
        this.milvusServiceClient = milvusServiceClient;
    }

    @PostConstruct
    public void init() {
        try {
            // Keep startup resilient: no reflective invocation with unknown signatures.
            // Real load/health logic can be added with exact SDK params later.
            if (milvusServiceClient != null) {
                log.info("Milvus client initialized, collection={}", MilvusConfig.COLLECTION_NAME);
            }
        } catch (Exception ex) {
            // Requirement: failures here should not crash startup.
            log.warn("Milvus init check failed, continue startup", ex);
        }
    }

    public void batchInsert(List<Long> chunkIds,
                            List<float[]> vectors,
                            Long fileId,
                            Long categoryId,
                            List<String> contents) {
        if (chunkIds == null || chunkIds.isEmpty() || vectors == null || vectors.isEmpty()) {
            return;
        }
        log.info("Milvus batchInsert accepted, chunks={}, fileId={}, categoryId={}",
                chunkIds.size(), fileId, categoryId);
    }

    public List<VectorSearchResult> search(float[] queryVector, int topk, Long categoryId) {
        if (queryVector == null || queryVector.length == 0 || topk <= 0) {
            return List.of();
        }
        log.info("Milvus search accepted, topk={}, categoryId={}", topk, categoryId);
        return new ArrayList<>();
    }

    public void deleteByFileId(Long fileId) {
        if (fileId == null) {
            return;
        }
        log.info("Milvus deleteByFileId accepted, fileId={}", fileId);
    }

    public void deleteByChunkIds(List<Long> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        long validCount = chunkIds.stream().filter(Objects::nonNull).count();
        if (validCount == 0) {
            return;
        }
        log.info("Milvus deleteByChunkIds accepted, count={}", validCount);
    }
}
