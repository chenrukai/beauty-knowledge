package com.beauty.knowledge.module.knowledge.domain.vo;

import com.beauty.knowledge.module.file.domain.entity.KbFile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class KnowledgeDetailVO {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private Long categoryId;
    private String type;
    private String coverUrl;
    private Integer status;
    private Integer viewCount;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<KbFile> files = new ArrayList<>();
}
