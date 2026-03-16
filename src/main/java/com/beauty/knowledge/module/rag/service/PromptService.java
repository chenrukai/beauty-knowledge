package com.beauty.knowledge.module.rag.service;

import com.beauty.knowledge.module.rag.domain.dto.ChatMessage;
import com.beauty.knowledge.module.rag.domain.dto.ChunkResult;
import com.beauty.knowledge.module.rag.domain.enums.IntentType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptService {

    public String buildSystemPrompt(IntentType intentType) {
        return switch (intentType) {
            case INGREDIENT -> "You are a professional beauty ingredient analyst.";
            case TECHNIQUE -> "You are a professional beauty operation instructor.";
            case PRODUCT -> "You are a professional beauty product consultant.";
            case GENERAL -> "You are a helpful beauty knowledge assistant.";
        };
    }

    public String buildUserPrompt(String question, List<ChunkResult> chunks, List<ChatMessage> history) {
        String context = buildContext(chunks);
        String historyText = buildHistory(history);
        return """
                References:
                %s

                Conversation history:
                %s

                Current question:
                %s

                Requirements:
                - Answer based on references when possible.
                - If references are insufficient, clearly say what is missing.
                - Keep answer concise and practical.
                """.formatted(context, historyText, question);
    }

    private String buildContext(List<ChunkResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "(No references)";
        }
        return chunks.stream()
                .map(c -> "---\nsource: %s page %s\n%s".formatted(
                        c.getFileName() == null ? "unknown" : c.getFileName(),
                        c.getPage() == null ? "-" : c.getPage(),
                        c.getContent()))
                .collect(Collectors.joining("\n\n"));
    }

    private String buildHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "(No history)";
        }
        return history.stream()
                .map(m -> "%s: %s".formatted(m.getRole(), m.getContent()))
                .collect(Collectors.joining("\n"));
    }
}
