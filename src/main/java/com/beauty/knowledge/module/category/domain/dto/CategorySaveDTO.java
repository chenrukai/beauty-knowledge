package com.beauty.knowledge.module.category.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategorySaveDTO {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Long parentId;

    private Integer sort;

    private String icon;

    private String description;
}
