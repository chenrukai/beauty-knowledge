package com.beauty.knowledge.module.knowledge.service;

import com.beauty.knowledge.common.result.PageResult;
import com.beauty.knowledge.module.knowledge.domain.dto.KnowledgePageDTO;
import com.beauty.knowledge.module.knowledge.domain.dto.KnowledgeSaveDTO;
import com.beauty.knowledge.module.knowledge.domain.entity.KbKnowledge;
import com.beauty.knowledge.module.knowledge.domain.vo.KnowledgeDetailVO;

public interface KnowledgeService {

    PageResult<KbKnowledge> page(KnowledgePageDTO dto);

    KnowledgeDetailVO getById(Long id);

    void save(KnowledgeSaveDTO dto);

    void update(Long id, KnowledgeSaveDTO dto);

    void remove(Long id);

    PageResult<KbKnowledge> search(String keyword, Long pageNum, Long pageSize);
}
