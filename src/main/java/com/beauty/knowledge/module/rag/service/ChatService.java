package com.beauty.knowledge.module.rag.service;

import com.beauty.knowledge.infrastructure.ai.llm.LLMProvider;
import com.beauty.knowledge.module.rag.domain.dto.ChatMessage;
import com.beauty.knowledge.module.rag.domain.dto.ChunkResult;
import com.beauty.knowledge.module.rag.domain.enums.IntentType;
import com.beauty.knowledge.module.rag.domain.request.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatService {

    private final IntentService intentService;
    private final HybridSearchService hybridSearchService;
    private final ContextService contextService;
    private final PromptService promptService;
    private final LLMProvider llmProvider;
    private final ChatSessionService chatSessionService;
    private final ChatRecordService chatRecordService;
    private final ObjectMapper objectMapper;

    private final Map<Long, List<ChunkResult>> lastChunksCache = new ConcurrentHashMap<>();

    public ChatService(IntentService intentService,
                       HybridSearchService hybridSearchService,
                       ContextService contextService,
                       PromptService promptService,
                       LLMProvider llmProvider,
                       ChatSessionService chatSessionService,
                       ChatRecordService chatRecordService,
                       ObjectMapper objectMapper) {
        this.intentService = intentService;
        this.hybridSearchService = hybridSearchService;
        this.contextService = contextService;
        this.promptService = promptService;
        this.llmProvider = llmProvider;
        this.chatSessionService = chatSessionService;
        this.chatRecordService = chatRecordService;
        this.objectMapper = objectMapper;
    }

    public Flux<String> chat(ChatRequest request, Long userId) {
        Long sessionId = chatSessionService.ensureSession(userId, request.getSessionId(), request.getQuestion());
        List<ChatMessage> history = contextService.getHistory(String.valueOf(sessionId));

        CompletableFuture<IntentType> intentFuture = CompletableFuture.supplyAsync(
                () -> intentService.recognize(request.getQuestion()));
        CompletableFuture<List<ChunkResult>> chunksFuture = CompletableFuture.supplyAsync(
                () -> hybridSearchService.search(request.getQuestion(), request.getCategoryId()));
        CompletableFuture.allOf(intentFuture, chunksFuture).join();

        IntentType intent = intentFuture.join();
        List<ChunkResult> chunks = chunksFuture.join();
        lastChunksCache.put(userId, chunks);

        String systemPrompt = promptService.buildSystemPrompt(intent);
        String userPrompt = promptService.buildUserPrompt(request.getQuestion(), chunks, history);

        StringBuilder answerBuffer = new StringBuilder();
        List<com.beauty.knowledge.infrastructure.ai.llm.dto.ChatMessage> providerHistory = new ArrayList<>();
        for (ChatMessage h : history) {
            providerHistory.add(new com.beauty.knowledge.infrastructure.ai.llm.dto.ChatMessage(h.getRole(), h.getContent()));
        }

        contextService.append(String.valueOf(sessionId), new ChatMessage("user", request.getQuestion()));
        chatRecordService.saveMessage(sessionId, "user", request.getQuestion(), null, intent.name());

        return llmProvider.streamChat(systemPrompt, userPrompt, providerHistory)
                .doOnNext(answerBuffer::append)
                .doOnComplete(() -> {
                    String answer = answerBuffer.toString();
                    contextService.append(String.valueOf(sessionId), new ChatMessage("assistant", answer));
                    String sourcesJson = toSourcesJson(chunks);
                    chatRecordService.saveMessage(sessionId, "assistant", answer, sourcesJson, intent.name());
                    chatSessionService.increaseMessageCount(sessionId, 2);
                });
    }

    public List<ChunkResult> getLastChunks(Long userId) {
        return lastChunksCache.getOrDefault(userId, List.of());
    }

    private String toSourcesJson(List<ChunkResult> chunks) {
        try {
            return objectMapper.writeValueAsString(chunks);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
