package com.beauty.knowledge.module.rag.controller;

import com.beauty.knowledge.common.result.Result;
import com.beauty.knowledge.common.util.SecurityUtil;
import com.beauty.knowledge.module.rag.domain.dto.ChunkResult;
import com.beauty.knowledge.module.rag.domain.request.ChatRequest;
import com.beauty.knowledge.module.rag.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@Tag(name = "智能问答")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "流式问答")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return chatService.chat(request, userId)
                .map(token -> ServerSentEvent.builder(token).event("token").build())
                .concatWith(Flux.defer(() -> {
                    List<ChunkResult> chunks = chatService.getLastChunks(userId);
                    String donePayload;
                    try {
                        donePayload = objectMapper.writeValueAsString(chunks);
                    } catch (JsonProcessingException e) {
                        donePayload = "[]";
                    }
                    return Flux.just(ServerSentEvent.builder(donePayload).event("done").build());
                }));
    }

    @Operation(summary = "最近一次引用来源")
    @GetMapping("/sources")
    public Result<List<ChunkResult>> sources() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(chatService.getLastChunks(userId));
    }
}
