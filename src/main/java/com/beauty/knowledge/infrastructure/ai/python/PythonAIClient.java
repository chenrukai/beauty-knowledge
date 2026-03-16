package com.beauty.knowledge.infrastructure.ai.python;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PythonAIClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${beauty.ai.python.base-url:http://localhost:8001}")
    private String baseUrl;

    @Value("${beauty.ai.python.timeout:30000}")
    private Long timeoutMillis;

    @Value("${beauty.pipeline.embed-batch-size:32}")
    private Integer embedBatchSize;

    public PythonAIClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        int batchSize = embedBatchSize == null || embedBatchSize <= 0 ? 32 : embedBatchSize;
        List<float[]> allVectors = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            allVectors.addAll(embedBatch(batch));
        }
        return allVectors;
    }

    public String ocr(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String resp = webClient.post()
                    .uri(baseUrl + "/ocr")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("imageBase64", base64))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .block();
            if (!StringUtils.hasText(resp)) {
                return "";
            }
            JsonNode root = objectMapper.readTree(resp);
            String text = root.path("text").asText("");
            if (StringUtils.hasText(text)) {
                return text;
            }
            return root.path("data").path("text").asText("");
        } catch (Exception ex) {
            log.warn("OCR failed, return empty string");
            return "";
        }
    }

    public boolean healthCheck() {
        try {
            return Boolean.TRUE.equals(webClient.get()
                    .uri(baseUrl + "/health")
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

    private List<float[]> embedBatch(List<String> batch) {
        try {
            String resp = webClient.post()
                    .uri(baseUrl + "/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("texts", batch))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .block();
            return parseVectors(resp);
        } catch (Exception ex) {
            log.error("Python embed failed, batchSize={}", batch.size(), ex);
            throw new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private List<float[]> parseVectors(String resp) throws Exception {
        if (!StringUtils.hasText(resp)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(resp);
        JsonNode vectorsNode = root.path("vectors");
        if (!vectorsNode.isArray()) {
            vectorsNode = root.path("data").path("vectors");
        }
        if (!vectorsNode.isArray()) {
            throw new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE);
        }

        List<float[]> vectors = new ArrayList<>();
        for (JsonNode vectorNode : vectorsNode) {
            if (!vectorNode.isArray()) {
                continue;
            }
            float[] vector = new float[vectorNode.size()];
            for (int i = 0; i < vectorNode.size(); i++) {
                vector[i] = (float) vectorNode.get(i).asDouble();
            }
            vectors.add(vector);
        }
        return vectors;
    }
}
