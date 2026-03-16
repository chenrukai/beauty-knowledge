package com.beauty.knowledge.module.file.domain.dto;

import lombok.Data;

@Data
public class TaskPageDTO {

    private Long pageNum = 1L;
    private Long pageSize = 20L;
    private String status;
    private String startDate;
    private String endDate;
}
