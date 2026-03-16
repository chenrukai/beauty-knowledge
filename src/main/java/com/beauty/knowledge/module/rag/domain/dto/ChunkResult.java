package com.beauty.knowledge.module.rag.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkResult {

    private Long chunkId;
    private String content;
    private Integer page;
    private Long fileId;
    private String fileName;
    private Double score;
}
