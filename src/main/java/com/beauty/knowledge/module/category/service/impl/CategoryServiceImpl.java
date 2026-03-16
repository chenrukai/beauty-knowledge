package com.beauty.knowledge.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;
import com.beauty.knowledge.module.category.domain.dto.CategorySaveDTO;
import com.beauty.knowledge.module.category.domain.entity.KbCategory;
import com.beauty.knowledge.module.category.domain.vo.CategoryTreeVO;
import com.beauty.knowledge.module.category.mapper.KbCategoryMapper;
import com.beauty.knowledge.module.category.service.CategoryService;
import com.beauty.knowledge.module.knowledge.domain.entity.KbKnowledge;
import com.beauty.knowledge.module.knowledge.mapper.KbKnowledgeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Long ROOT_PARENT_ID = 0L;

    private final KbCategoryMapper kbCategoryMapper;
    private final KbKnowledgeMapper kbKnowledgeMapper;

    public CategoryServiceImpl(KbCategoryMapper kbCategoryMapper,
                               KbKnowledgeMapper kbKnowledgeMapper) {
        this.kbCategoryMapper = kbCategoryMapper;
        this.kbKnowledgeMapper = kbKnowledgeMapper;
    }

    @Override
    public List<CategoryTreeVO> getTree() {
        LambdaQueryWrapper<KbCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(KbCategory::getSort).orderByAsc(KbCategory::getId);
        List<KbCategory> allCategories = kbCategoryMapper.selectList(queryWrapper);

        return allCategories.stream()
                .filter(category -> ROOT_PARENT_ID.equals(defaultParentId(category.getParentId())))
                .sorted(categoryComparator())
                .map(category -> toTree(category, allCategories))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CategorySaveDTO dto) {
        KbCategory category = new KbCategory();
        category.setName(dto.getName());
        category.setParentId(defaultParentId(dto.getParentId()));
        category.setSort(defaultSort(dto.getSort()));
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setLevel(resolveLevel(category.getParentId()));
        kbCategoryMapper.insert(category);
        log.info("Category created, id={}, name={}", category.getId(), category.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveDTO dto) {
        KbCategory existing = kbCategoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }

        Long parentId = defaultParentId(dto.getParentId());
        existing.setName(dto.getName());
        existing.setParentId(parentId);
        existing.setSort(defaultSort(dto.getSort()));
        existing.setIcon(dto.getIcon());
        existing.setDescription(dto.getDescription());
        existing.setLevel(resolveLevel(parentId));
        kbCategoryMapper.updateById(existing);
        log.info("Category updated, id={}, name={}", existing.getId(), existing.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        LambdaQueryWrapper<KbCategory> childQueryWrapper = new LambdaQueryWrapper<>();
        childQueryWrapper.eq(KbCategory::getParentId, id);
        Long childCount = kbCategoryMapper.selectCount(childQueryWrapper);
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_HAS_CHILDREN);
        }

        LambdaQueryWrapper<KbKnowledge> knowledgeQueryWrapper = new LambdaQueryWrapper<>();
        knowledgeQueryWrapper.eq(KbKnowledge::getCategoryId, id);
        Long knowledgeCount = kbKnowledgeMapper.selectCount(knowledgeQueryWrapper);
        if (knowledgeCount != null && knowledgeCount > 0) {
            throw new BusinessException(ResultCode.CATEGORY_HAS_CHILDREN);
        }

        kbCategoryMapper.deleteById(id);
        log.info("Category removed, id={}", id);
    }

    private CategoryTreeVO toTree(KbCategory current, List<KbCategory> allCategories) {
        List<CategoryTreeVO> children = allCategories.stream()
                .filter(category -> Objects.equals(defaultParentId(category.getParentId()), current.getId()))
                .sorted(categoryComparator())
                .map(child -> toTree(child, allCategories))
                .collect(Collectors.toList());

        return CategoryTreeVO.builder()
                .id(current.getId())
                .name(current.getName())
                .parentId(current.getParentId())
                .level(current.getLevel())
                .sort(current.getSort())
                .icon(current.getIcon())
                .description(current.getDescription())
                .children(children)
                .build();
    }

    private Comparator<KbCategory> categoryComparator() {
        return Comparator.comparing(KbCategory::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(KbCategory::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Long defaultParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private Integer defaultSort(Integer sort) {
        return sort == null ? 0 : sort;
    }

    private Integer resolveLevel(Long parentId) {
        if (ROOT_PARENT_ID.equals(parentId)) {
            return 1;
        }
        KbCategory parent = kbCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "父分类不存在");
        }
        return parent.getLevel() == null ? 2 : parent.getLevel() + 1;
    }
}
