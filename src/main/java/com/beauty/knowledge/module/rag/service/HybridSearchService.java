package com.beauty.knowledge.module.rag.service;

import com.beauty.knowledge.infrastructure.ai.python.PythonAIClient;
import com.beauty.knowledge.infrastructure.vector.MilvusVectorStore;
import com.beauty.knowledge.infrastructure.vector.VectorSearchResult;
import com.beauty.knowledge.module.rag.domain.dto.ChunkResult;
import com.beauty.knowledge.module.rag.mapper.KbChunkSearchMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HybridSearchService {

    @Value("${beauty.rag.rrf-k:60}")
    private int rrfK;
    @Value("${beauty.rag.bm25-topk:5}")
    private int bm25Topk;
    @Value("${beauty.rag.vector-topk:5}")
    private int vectorTopk;
    @Value("${beauty.rag.rerank-top:5}")
    private int rerankTop;

    private final KbChunkSearchMapper kbChunkSearchMapper;
    private final PythonAIClient pythonAIClient;
    private final MilvusVectorStore milvusVectorStore;

    public HybridSearchService(KbChunkSearchMapper kbChunkSearchMapper,
                               PythonAIClient pythonAIClient,
                               MilvusVectorStore milvusVectorStore) {
        this.kbChunkSearchMapper = kbChunkSearchMapper;
        this.pythonAIClient = pythonAIClient;
        this.milvusVectorStore = milvusVectorStore;
    }

    public List<ChunkResult> search(String question, Long categoryId) {
        CompletableFuture<List<Long>> bm25Future =
                CompletableFuture.supplyAsync(() -> bm25Search(question, categoryId));
        CompletableFuture<List<Long>> vectorFuture =
                CompletableFuture.supplyAsync(() -> vectorSearch(question, categoryId));
        CompletableFuture.allOf(bm25Future, vectorFuture).join();

        List<Long> bm25Ids = bm25Future.join();
        List<Long> vectorIds = vectorFuture.join();
        Map<Long, Double> rrfScores = computeRrf(bm25Ids, vectorIds);

        List<Long> topIds = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(rerankTop)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (topIds.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> rows = kbChunkSearchMapper.findByIdsWithFile(topIds);
        Map<Long, Double> scoreMap = rrfScores;
        return rows.stream()
                .map(row -> ChunkResult.builder()
                        .chunkId(((Number) row.get("id")).longValue())
                        .content((String) row.get("content"))
                        .page(row.get("page") == null ? null : ((Number) row.get("page")).intValue())
                        .fileId(((Number) row.get("file_id")).longValue())
                        .fileName((String) row.get("file_name"))
                        .score(scoreMap.getOrDefault(((Number) row.get("id")).longValue(), 0D))
                        .build())
                .collect(Collectors.toList());
    }

    private List<Long> bm25Search(String question, Long categoryId) {
        try {
            return kbChunkSearchMapper.bm25Search(question, categoryId, bm25Topk);
        } catch (Exception ex) {
            log.warn("BM25 search failed");
            return List.of();
        }
    }

    private List<Long> vectorSearch(String question, Long categoryId) {
        try {
            List<float[]> vectors = pythonAIClient.embed(List.of(question));
            if (vectors.isEmpty()) {
                return List.of();
            }
            List<VectorSearchResult> searchResults = milvusVectorStore.search(vectors.get(0), vectorTopk, categoryId);
            return searchResults.stream().map(VectorSearchResult::getChunkId).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Vector search failed");
            return List.of();
        }
    }

    private Map<Long, Double> computeRrf(List<Long> bm25Ids, List<Long> vectorIds) {
        Map<Long, Double> scores = new HashMap<>();
        for (int i = 0; i < bm25Ids.size(); i++) {
            Long id = bm25Ids.get(i);
            scores.merge(id, 1D / (rrfK + i + 1), Double::sum);
        }
        for (int i = 0; i < vectorIds.size(); i++) {
            Long id = vectorIds.get(i);
            scores.merge(id, 1D / (rrfK + i + 1), Double::sum);
        }
        return scores;
    }
}
