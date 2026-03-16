package com.beauty.knowledge.module.category.service;

import com.beauty.knowledge.module.category.domain.dto.CategorySaveDTO;
import com.beauty.knowledge.module.category.domain.vo.CategoryTreeVO;

import java.util.List;

public interface CategoryService {

    List<CategoryTreeVO> getTree();

    void save(CategorySaveDTO dto);

    void update(Long id, CategorySaveDTO dto);

    void remove(Long id);
}
