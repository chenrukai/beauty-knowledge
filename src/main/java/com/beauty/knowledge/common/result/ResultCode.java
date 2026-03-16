package com.beauty.knowledge.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error"),

    FILE_HASH_DUPLICATE(1001, "File hash duplicate"),
    FILE_PROCESSING(1002, "File is processing"),
    KNOWLEDGE_NOT_FOUND(1003, "Knowledge not found"),
    ENTITY_NAME_EXISTS(1004, "Entity name already exists"),
    CATEGORY_HAS_CHILDREN(1005, "Category has children"),
    AI_SERVICE_UNAVAILABLE(1006, "AI service unavailable");

    private final Integer code;
    private final String message;
}
