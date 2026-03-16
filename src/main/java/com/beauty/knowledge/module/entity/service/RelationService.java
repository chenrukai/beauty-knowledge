package com.beauty.knowledge.module.entity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.module.entity.domain.entity.BeautyEffect;
import com.beauty.knowledge.module.entity.domain.entity.BeautyIngredient;
import com.beauty.knowledge.module.entity.domain.entity.RelIngredientEffect;
import com.beauty.knowledge.module.entity.domain.entity.RelProductIngredient;
import com.beauty.knowledge.module.entity.mapper.BeautyEffectMapper;
import com.beauty.knowledge.module.entity.mapper.BeautyIngredientMapper;
import com.beauty.knowledge.module.entity.mapper.RelIngredientEffectMapper;
import com.beauty.knowledge.module.entity.mapper.RelProductIngredientMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RelationService {

    private final RelIngredientEffectMapper ingredientEffectMapper;
    private final RelProductIngredientMapper productIngredientMapper;
    private final BeautyEffectMapper effectMapper;
    private final BeautyIngredientMapper ingredientMapper;

    public RelationService(RelIngredientEffectMapper ingredientEffectMapper,
                           RelProductIngredientMapper productIngredientMapper,
                           BeautyEffectMapper effectMapper,
                           BeautyIngredientMapper ingredientMapper) {
        this.ingredientEffectMapper = ingredientEffectMapper;
        this.productIngredientMapper = productIngredientMapper;
        this.effectMapper = effectMapper;
        this.ingredientMapper = ingredientMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindIngredientEffect(Long ingredientId, Long effectId, String strength) {
        Long count = ingredientEffectMapper.selectCount(new LambdaQueryWrapper<RelIngredientEffect>()
                .eq(RelIngredientEffect::getIngredientId, ingredientId)
                .eq(RelIngredientEffect::getEffectId, effectId));
        if (count != null && count > 0) {
            return;
        }
        RelIngredientEffect rel = new RelIngredientEffect();
        rel.setIngredientId(ingredientId);
        rel.setEffectId(effectId);
        rel.setStrength(strength);
        ingredientEffectMapper.insert(rel);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindIngredientEffect(Long ingredientId, Long effectId) {
        ingredientEffectMapper.delete(new LambdaQueryWrapper<RelIngredientEffect>()
                .eq(RelIngredientEffect::getIngredientId, ingredientId)
                .eq(RelIngredientEffect::getEffectId, effectId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindProductIngredient(Long productId, Long ingredientId, String concentration, boolean isKey) {
        Long count = productIngredientMapper.selectCount(new LambdaQueryWrapper<RelProductIngredient>()
                .eq(RelProductIngredient::getProductId, productId)
                .eq(RelProductIngredient::getIngredientId, ingredientId));
        if (count != null && count > 0) {
            return;
        }
        RelProductIngredient rel = new RelProductIngredient();
        rel.setProductId(productId);
        rel.setIngredientId(ingredientId);
        rel.setConcentration(concentration);
        rel.setIsKey(isKey ? 1 : 0);
        productIngredientMapper.insert(rel);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindProductIngredient(Long productId, Long ingredientId) {
        productIngredientMapper.delete(new LambdaQueryWrapper<RelProductIngredient>()
                .eq(RelProductIngredient::getProductId, productId)
                .eq(RelProductIngredient::getIngredientId, ingredientId));
    }

    public List<BeautyEffect> getEffectsByIngredient(Long ingredientId) {
        List<Long> effectIds = ingredientEffectMapper.selectList(new LambdaQueryWrapper<RelIngredientEffect>()
                        .eq(RelIngredientEffect::getIngredientId, ingredientId))
                .stream()
                .map(RelIngredientEffect::getEffectId)
                .collect(Collectors.toList());
        if (effectIds.isEmpty()) {
            return List.of();
        }
        return effectMapper.selectBatchIds(effectIds);
    }

    public List<BeautyIngredient> getIngredientsByProduct(Long productId) {
        List<Long> ingredientIds = productIngredientMapper.selectList(new LambdaQueryWrapper<RelProductIngredient>()
                        .eq(RelProductIngredient::getProductId, productId))
                .stream()
                .map(RelProductIngredient::getIngredientId)
                .collect(Collectors.toList());
        if (ingredientIds.isEmpty()) {
            return List.of();
        }
        return ingredientMapper.selectBatchIds(ingredientIds);
    }
}
