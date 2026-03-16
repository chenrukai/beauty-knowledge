package com.beauty.knowledge.module.pipeline.service;

import com.beauty.knowledge.module.pipeline.domain.dto.ChunkDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ChunkService {

    @Value("${beauty.pipeline.chunk-size:400}")
    private int chunkSize;

    @Value("${beauty.pipeline.chunk-overlap:80}")
    private int chunkOverlap;

    public List<ChunkDTO> split(String text, int totalPages) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        String source = text.trim();
        int safeChunk = Math.max(chunkSize, 100);
        int safeOverlap = Math.max(0, Math.min(chunkOverlap, safeChunk - 1));
        int step = safeChunk - safeOverlap;

        List<ChunkDTO> result = new ArrayList<>();
        int index = 0;
        for (int start = 0; start < source.length(); start += step) {
            int end = Math.min(start + safeChunk, source.length());
            String chunk = source.substring(start, end);
            if (!StringUtils.hasText(chunk)) {
                continue;
            }
            int page = estimatePage(start, source.length(), Math.max(totalPages, 1));
            result.add(ChunkDTO.builder()
                    .chunkIndex(index++)
                    .content(chunk)
                    .page(page)
                    .charCount(chunk.length())
                    .build());
            if (end >= source.length()) {
                break;
            }
        }
        log.info("Text chunk split completed, chunks={}", result.size());
        return result;
    }

    private int estimatePage(int position, int totalLength, int totalPages) {
        if (totalLength <= 0) {
            return 1;
        }
        double ratio = (double) position / totalLength;
        int page = (int) Math.floor(ratio * totalPages) + 1;
        return Math.max(1, Math.min(page, totalPages));
    }
}
