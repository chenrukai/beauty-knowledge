package com.beauty.knowledge.common.constant;

public interface RedisKeyConstant {

    String CHAT_SESSION_TEMPLATE = "chat:session:%s";
    String USER_TOKEN_TEMPLATE = "user:token:%s";
    String KNOWLEDGE_HOT_TEMPLATE = "knowledge:hot:%s";
    String FILE_HASH_TEMPLATE = "file:hash:%s";
    String TASK_STATUS_TEMPLATE = "task:status:%s";
    String CATEGORY_TREE_TEMPLATE = "category:tree:%s";
    String RATE_LIMIT_CHAT_TEMPLATE = "rate:limit:chat:%s";

    long CHAT_SESSION_TTL_SECONDS = 7 * 24 * 60 * 60L;
    long USER_TOKEN_TTL_SECONDS = 24 * 60 * 60L;
    long KNOWLEDGE_HOT_TTL_SECONDS = 60 * 60L;
    long FILE_HASH_TTL_SECONDS = 24 * 60 * 60L;
    long TASK_STATUS_TTL_SECONDS = 24 * 60 * 60L;
    long CATEGORY_TREE_TTL_SECONDS = 30 * 60L;
    long RATE_LIMIT_CHAT_TTL_SECONDS = 60L;

    static String chatSession(Long userId) {
        return CHAT_SESSION_TEMPLATE.formatted(userId);
    }

    static String userToken(Long userId) {
        return USER_TOKEN_TEMPLATE.formatted(userId);
    }

    static String knowledgeHot(Long knowledgeId) {
        return KNOWLEDGE_HOT_TEMPLATE.formatted(knowledgeId);
    }

    static String fileHash(String hash) {
        return FILE_HASH_TEMPLATE.formatted(hash);
    }

    static String taskStatus(Long taskId) {
        return TASK_STATUS_TEMPLATE.formatted(taskId);
    }

    static String categoryTree(String root) {
        return CATEGORY_TREE_TEMPLATE.formatted(root);
    }

    static String rateLimitChat(Long userId) {
        return RATE_LIMIT_CHAT_TEMPLATE.formatted(userId);
    }
}
