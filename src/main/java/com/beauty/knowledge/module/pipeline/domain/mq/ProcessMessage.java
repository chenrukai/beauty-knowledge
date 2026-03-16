package com.beauty.knowledge.module.pipeline.domain.mq;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessMessage {

    private Long fileId;
    private String fileType;
    private String minioPath;
    private Long categoryId;
    private Long knowledgeId;

    @Builder.Default
    private Integer retryCount = 0;
}
