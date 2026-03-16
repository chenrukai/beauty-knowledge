package com.beauty.knowledge.common.exception;

import com.beauty.knowledge.common.result.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    FILE_HASH_DUPLICATE(ResultCode.FILE_HASH_DUPLICATE.getCode(), ResultCode.FILE_HASH_DUPLICATE.getMessage()),
    FILE_PROCESSING(ResultCode.FILE_PROCESSING.getCode(), ResultCode.FILE_PROCESSING.getMessage()),
    KNOWLEDGE_NOT_FOUND(ResultCode.KNOWLEDGE_NOT_FOUND.getCode(), ResultCode.KNOWLEDGE_NOT_FOUND.getMessage()),
    CATEGORY_HAS_CHILDREN(ResultCode.CATEGORY_HAS_CHILDREN.getCode(), ResultCode.CATEGORY_HAS_CHILDREN.getMessage()),
    BAD_REQUEST(ResultCode.BAD_REQUEST.getCode(), ResultCode.BAD_REQUEST.getMessage()),
    UNAUTHORIZED(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()),
    FORBIDDEN(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage()),
    NOT_FOUND(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage()),
    INTERNAL_ERROR(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage());

    private final Integer code;
    private final String message;
}
