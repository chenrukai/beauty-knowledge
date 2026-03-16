package com.beauty.knowledge.module.entity.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("rel_product_ingredient")
public class RelProductIngredient {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long ingredientId;
    private String concentration;
    private Integer isKey;
}
