package com.beauty.knowledge.module.category.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CategoryTreeVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;
    private String description;

    @Builder.Default
    private List<CategoryTreeVO> children = new ArrayList<>();
}
