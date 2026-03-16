package com.beauty.knowledge.module.knowledge.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_chunk")
public class KbChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private Long knowledgeId;

    private Integer chunkIndex;

    private String content;

    private Integer page;

    private Integer charCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
