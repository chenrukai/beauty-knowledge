package com.beauty.knowledge.module.rag.service;

import com.beauty.knowledge.module.rag.domain.entity.ChatMessageEntity;
import com.beauty.knowledge.module.rag.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRecordService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatRecordService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(Long sessionId, String role, String content, String refSources, String intent) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setRefSources(refSources);
        entity.setIntent(intent);
        chatMessageMapper.insert(entity);
    }
}
