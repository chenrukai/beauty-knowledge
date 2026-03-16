package com.beauty.knowledge.common.util;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChunkUtil {

    public static final int CHUNK_SIZE = 400;
    public static final int CHUNK_OVERLAP = 80;

    private ChunkUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<String> split(String text, int size, int overlap) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        if (size <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Chunk size must be greater than 0");
        }
        if (overlap < 0 || overlap >= size) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Chunk overlap must be >= 0 and < chunk size");
        }

        List<String> chunks = new ArrayList<>();
        int step = size - overlap;
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + size, length);
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end == length) {
                break;
            }
            start += step;
        }
        return chunks;
    }

    public static List<String> split(String text) {
        return split(text, CHUNK_SIZE, CHUNK_OVERLAP);
    }
}
