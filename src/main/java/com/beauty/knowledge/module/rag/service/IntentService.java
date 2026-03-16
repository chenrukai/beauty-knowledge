package com.beauty.knowledge.module.rag.service;

import com.beauty.knowledge.infrastructure.ai.llm.LLMProvider;
import com.beauty.knowledge.module.rag.domain.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IntentService {

    private static final String INTENT_PROMPT = """
            Classify the question into one intent and return only label:
            - INGREDIENT
            - TECHNIQUE
            - PRODUCT
            - GENERAL
            Question: %s
            """;

    private final LLMProvider llmProvider;

    public IntentService(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public IntentType recognize(String question) {
        try {
            String response = llmProvider.chat("You are an intent classifier.", INTENT_PROMPT.formatted(question));
            String upper = response == null ? "" : response.toUpperCase();
            if (upper.contains("INGREDIENT")) {
                return IntentType.INGREDIENT;
            }
            if (upper.contains("TECHNIQUE")) {
                return IntentType.TECHNIQUE;
            }
            if (upper.contains("PRODUCT")) {
                return IntentType.PRODUCT;
            }
            return IntentType.GENERAL;
        } catch (Exception ex) {
            log.warn("Intent recognition failed, fallback GENERAL");
            return IntentType.GENERAL;
        }
    }
}
