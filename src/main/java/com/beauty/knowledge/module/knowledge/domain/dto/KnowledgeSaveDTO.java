package com.beauty.knowledge.module.knowledge.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeSaveDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "类型不能为空")
    private String type;

    private String content;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
