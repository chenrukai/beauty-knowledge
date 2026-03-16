package com.beauty.knowledge.module.entity.domain.dto;

import lombok.Data;

@Data
public class PendingPageDTO {

    private Long pageNum = 1L;
    private Long pageSize = 20L;
    private String entityType;
    private String extractMethod;
}
