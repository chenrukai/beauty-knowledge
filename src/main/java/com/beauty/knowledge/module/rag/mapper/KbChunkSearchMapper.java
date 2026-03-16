package com.beauty.knowledge.module.rag.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface KbChunkSearchMapper {

    List<Long> bm25Search(@Param("keyword") String keyword,
                          @Param("categoryId") Long categoryId,
                          @Param("topk") Integer topk);

    List<Map<String, Object>> findByIdsWithFile(@Param("ids") List<Long> ids);
}
