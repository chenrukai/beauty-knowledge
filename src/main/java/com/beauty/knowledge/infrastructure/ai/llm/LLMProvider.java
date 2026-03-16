package com.beauty.knowledge.infrastructure.ai.llm;

import com.beauty.knowledge.infrastructure.ai.llm.dto.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LLMProvider {

    Flux<String> streamChat(String systemPrompt, String userMessage, List<ChatMessage> history);

    String chat(String systemPrompt, String userMessage);

    boolean isAvailable();
}
