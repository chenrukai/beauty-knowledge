package com.beauty.knowledge.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beauty.knowledge.module.knowledge.domain.entity.KbKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbKnowledgeMapper extends BaseMapper<KbKnowledge> {

    List<KbKnowledge> fullTextSearch(@Param("keyword") String keyword,
                                     @Param("offset") long offset,
                                     @Param("pageSize") long pageSize);

    Long fullTextSearchCount(@Param("keyword") String keyword);
}
