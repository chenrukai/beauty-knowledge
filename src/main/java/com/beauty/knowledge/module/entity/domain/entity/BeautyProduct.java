package com.beauty.knowledge.module.entity.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("beauty_product")
public class BeautyProduct {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String brand;
    private String category;
    private String priceRange;
    private String skinType;
    private String description;
    private Long knowledgeId;
    private Integer confirmed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
