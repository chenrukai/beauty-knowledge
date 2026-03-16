package com.beauty.knowledge.infrastructure.ai.llm;

import com.beauty.knowledge.infrastructure.ai.llm.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "beauty.llm.provider", havingValue = "qwen")
public class QwenProvider implements LLMProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${beauty.llm.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${beauty.llm.qwen.api-key:}")
    private String apiKey;

    @Value("${beauty.llm.qwen.model:qwen-max}")
    private String model;

    @Value("${beauty.llm.qwen.timeout:30000}")
    private Long timeoutMillis;

    public QwenProvider(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<String> streamChat(String systemPrompt, String userMessage, List<ChatMessage> history) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", true,
                "messages", buildMessages(systemPrompt, userMessage, history)
        );
        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeoutMillis))
                .flatMap(this::extractStreamContent)
                .onErrorResume(ex -> {
                    log.error("Qwen stream chat failed", ex);
                    return Flux.empty();
                });
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", buildMessages(systemPrompt, userMessage, List.of())
        );
        try {
            String resp = webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .block();
            return extractSingleContent(resp);
        } catch (Exception ex) {
            log.error("Qwen chat failed", ex);
            return "";
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(webClient.get()
                    .uri(baseUrl + "/models")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .map(StringUtils::hasText)
                    .onErrorReturn(false)
                    .block());
        } catch (Exception ex) {
            return false;
        }
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userMessage, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        if (history != null) {
            for (ChatMessage m : history) {
                if (m != null && StringUtils.hasText(m.getRole()) && StringUtils.hasText(m.getContent())) {
                    messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage == null ? "" : userMessage));
        return messages;
    }

    private Flux<String> extractStreamContent(String rawLine) {
        if (!StringUtils.hasText(rawLine)) {
            return Flux.empty();
        }
        String line = rawLine.trim();
        if (line.startsWith("data:")) {
            line = line.substring(5).trim();
        }
        if ("[DONE]".equals(line)) {
            return Flux.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(line);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            if (content.isTextual() && StringUtils.hasText(content.asText())) {
                return Flux.just(content.asText());
            }
        } catch (Exception ignored) {
            // ignore malformed chunk
        }
        return Flux.empty();
    }

    private String extractSingleContent(String resp) {
        if (!StringUtils.hasText(resp)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isTextual() ? content.asText("") : "";
        } catch (Exception ex) {
            log.warn("Failed to parse qwen response");
            return "";
        }
    }
}
