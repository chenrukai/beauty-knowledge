package com.beauty.knowledge.module.entity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import com.beauty.knowledge.module.entity.domain.entity.BeautyIngredient;
import com.beauty.knowledge.module.entity.mapper.BeautyIngredientMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IngredientService {

    private final BeautyIngredientMapper ingredientMapper;

    public IngredientService(BeautyIngredientMapper ingredientMapper) {
        this.ingredientMapper = ingredientMapper;
    }

    public List<BeautyIngredient> list(String keyword) {
        LambdaQueryWrapper<BeautyIngredient> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null && !keyword.isBlank(), BeautyIngredient::getName, keyword)
                .orderByDesc(BeautyIngredient::getId);
        return ingredientMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BeautyIngredient ingredient) {
        Long count = ingredientMapper.selectCount(new LambdaQueryWrapper<BeautyIngredient>()
                .eq(BeautyIngredient::getName, ingredient.getName()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ENTITY_NAME_EXISTS);
        }
        ingredientMapper.insert(ingredient);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BeautyIngredient ingredient) {
        ingredient.setId(id);
        ingredientMapper.updateById(ingredient);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        ingredientMapper.deleteById(id);
    }
}
