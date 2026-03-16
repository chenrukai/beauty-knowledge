package com.beauty.knowledge.module.entity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.infrastructure.ai.llm.LLMProvider;
import com.beauty.knowledge.infrastructure.dictionary.BeautyDictionary;
import com.beauty.knowledge.module.entity.domain.dto.EntityExtractResult;
import com.beauty.knowledge.module.entity.domain.entity.EntityExtractPending;
import com.beauty.knowledge.module.entity.mapper.EntityExtractPendingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EntityExtractService {

    private static final int SEGMENT_MAX_LEN = 1000;

    private final EntityExtractPendingMapper pendingMapper;
    private final BeautyDictionary beautyDictionary;
    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper;

    public EntityExtractService(EntityExtractPendingMapper pendingMapper,
                                BeautyDictionary beautyDictionary,
                                LLMProvider llmProvider,
                                ObjectMapper objectMapper) {
        this.pendingMapper = pendingMapper;
        this.beautyDictionary = beautyDictionary;
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    public void extractAsync(Long fileId, String text) {
        if (!StringUtils.hasText(text) || fileId == null) {
            return;
        }
        saveDictionaryMatches(fileId, text);
        CompletableFuture.runAsync(() -> saveLlmExtract(fileId, text));
    }

    private void saveDictionaryMatches(Long fileId, String text) {
        List<String> hits = beautyDictionary.match(text);
        for (String hit : hits) {
            savePendingIfAbsent(fileId, "ingredient", hit, text, "dict");
        }
    }

    private void saveLlmExtract(Long fileId, String text) {
        try {
            for (String segment : splitSegments(text, SEGMENT_MAX_LEN)) {
                String prompt = """
                        Extract beauty entities from text and return JSON only:
                        {"ingredient":["..."],"effect":["..."],"product":["..."]}
                        text:
                        %s
                        """.formatted(segment);
                String resp = llmProvider.chat("You are an information extraction assistant.", prompt);
                if (!StringUtils.hasText(resp)) {
                    continue;
                }
                EntityExtractResult result = objectMapper.readValue(resp, EntityExtractResult.class);
                for (String name : result.getIngredient()) {
                    savePendingIfAbsent(fileId, "ingredient", name, segment, "llm");
                }
                for (String name : result.getEffect()) {
                    savePendingIfAbsent(fileId, "effect", name, segment, "llm");
                }
                for (String name : result.getProduct()) {
                    savePendingIfAbsent(fileId, "product", name, segment, "llm");
                }
            }
        } catch (Exception ex) {
            log.warn("LLM extract failed, fileId={}", fileId);
        }
    }

    private void savePendingIfAbsent(Long fileId, String type, String name, String sourceText, String method) {
        if (!StringUtils.hasText(name)) {
            return;
        }
        Long count = pendingMapper.selectCount(new LambdaQueryWrapper<EntityExtractPending>()
                .eq(EntityExtractPending::getFileId, fileId)
                .eq(EntityExtractPending::getEntityType, type)
                .eq(EntityExtractPending::getEntityName, name));
        if (count != null && count > 0) {
            return;
        }
        EntityExtractPending pending = new EntityExtractPending();
        pending.setFileId(fileId);
        pending.setEntityType(type);
        pending.setEntityName(name.trim());
        pending.setSourceText(sourceText);
        pending.setExtractMethod(method);
        pending.setStatus("PENDING");
        pendingMapper.insert(pending);
    }

    private List<String> splitSegments(String text, int maxLen) {
        List<String> segments = new ArrayList<>();
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) {
            return segments;
        }
        for (int i = 0; i < source.length(); i += maxLen) {
            int end = Math.min(i + maxLen, source.length());
            segments.add(source.substring(i, end));
        }
        return segments;
    }
}
