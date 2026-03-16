package com.beauty.knowledge.module.entity.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("entity_extract_pending")
public class EntityExtractPending {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private String entityType;
    private String entityName;
    private String sourceText;
    private String extractMethod;
    private String status;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
