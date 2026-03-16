package com.beauty.knowledge.infrastructure.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {

    private Long chunkId;
    private double score;
    private int rank;
    private String content;
}
