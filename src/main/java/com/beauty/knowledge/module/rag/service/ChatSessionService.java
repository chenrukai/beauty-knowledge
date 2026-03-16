package com.beauty.knowledge.module.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.module.rag.domain.entity.ChatSession;
import com.beauty.knowledge.module.rag.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;

    public ChatSessionService(ChatSessionMapper chatSessionMapper) {
        this.chatSessionMapper = chatSessionMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long ensureSession(Long userId, String sessionId, String title) {
        if (StringUtils.hasText(sessionId)) {
            try {
                Long id = Long.parseLong(sessionId);
                ChatSession existing = chatSessionMapper.selectById(id);
                if (existing != null && userId.equals(existing.getUserId())) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // fallback create
            }
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(title) ? title : "New Chat");
        session.setMessageCount(0);
        chatSessionMapper.insert(session);
        return session.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseMessageCount(Long sessionId, int add) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        int current = session.getMessageCount() == null ? 0 : session.getMessageCount();
        session.setMessageCount(current + Math.max(add, 0));
        chatSessionMapper.updateById(session);
    }

    public ChatSession getById(Long sessionId) {
        List<ChatSession> list = chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId));
        return list.isEmpty() ? null : list.get(0);
    }
}
