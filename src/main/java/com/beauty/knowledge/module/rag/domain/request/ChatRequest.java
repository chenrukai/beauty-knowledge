package com.beauty.knowledge.module.rag.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatRequest {

    @NotBlank(message = "Question is required")
    private String question;
    private String sessionId;
    private Long categoryId;
}
