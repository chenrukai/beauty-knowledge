package com.beauty.knowledge.module.rag.service;

import com.beauty.knowledge.common.constant.RedisKeyConstant;
import com.beauty.knowledge.module.rag.domain.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ContextService {

    private static final int MAX_HISTORY = 6;
    private static final long TTL_SECONDS = 1800L;

    private static final String LUA_UPDATE = """
            redis.call('RPUSH',  KEYS[1], ARGV[1])
            redis.call('LTRIM',  KEYS[1], -tonumber(ARGV[2]), -1)
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
            return 1
            """;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ContextService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ChatMessage> getHistory(String sessionId) {
        List<String> raw = redisTemplate.opsForList().range(buildKey(sessionId), 0, -1);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessage> history = new ArrayList<>();
        for (String json : raw) {
            try {
                history.add(objectMapper.readValue(json, ChatMessage.class));
            } catch (Exception ex) {
                log.warn("Parse history record failed");
            }
        }
        return history;
    }

    public void append(String sessionId, ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(LUA_UPDATE);
            script.setResultType(Long.class);
            redisTemplate.execute(script,
                    List.of(buildKey(sessionId)),
                    json,
                    String.valueOf(MAX_HISTORY),
                    String.valueOf(TTL_SECONDS));
        } catch (Exception ex) {
            log.warn("Update history failed, sessionId={}", sessionId);
        }
    }

    public void clearHistory(String sessionId) {
        redisTemplate.delete(buildKey(sessionId));
    }

    private String buildKey(String sessionId) {
        return RedisKeyConstant.chatHistory(sessionId);
    }
}
