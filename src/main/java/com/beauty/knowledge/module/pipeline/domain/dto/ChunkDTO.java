package com.beauty.knowledge.module.pipeline.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkDTO {

    private Integer chunkIndex;
    private String content;
    private Integer page;
    private Integer charCount;
}
