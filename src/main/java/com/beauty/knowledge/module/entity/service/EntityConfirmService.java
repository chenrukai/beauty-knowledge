package com.beauty.knowledge.module.entity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beauty.knowledge.common.util.SecurityUtil;
import com.beauty.knowledge.module.entity.domain.entity.BeautyEffect;
import com.beauty.knowledge.module.entity.domain.entity.BeautyIngredient;
import com.beauty.knowledge.module.entity.domain.entity.BeautyProduct;
import com.beauty.knowledge.module.entity.domain.entity.EntityExtractPending;
import com.beauty.knowledge.module.entity.mapper.BeautyEffectMapper;
import com.beauty.knowledge.module.entity.mapper.BeautyIngredientMapper;
import com.beauty.knowledge.module.entity.mapper.BeautyProductMapper;
import com.beauty.knowledge.module.entity.mapper.EntityExtractPendingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntityConfirmService {

    private final EntityExtractPendingMapper pendingMapper;
    private final BeautyIngredientMapper ingredientMapper;
    private final BeautyEffectMapper effectMapper;
    private final BeautyProductMapper productMapper;

    public EntityConfirmService(EntityExtractPendingMapper pendingMapper,
                                BeautyIngredientMapper ingredientMapper,
                                BeautyEffectMapper effectMapper,
                                BeautyProductMapper productMapper) {
        this.pendingMapper = pendingMapper;
        this.ingredientMapper = ingredientMapper;
        this.effectMapper = effectMapper;
        this.productMapper = productMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public int confirm(List<Long> ids, String action) {
        List<EntityExtractPending> pendingList = pendingMapper.selectList(
                new LambdaQueryWrapper<EntityExtractPending>()
                        .in(EntityExtractPending::getId, ids)
                        .eq(EntityExtractPending::getStatus, "PENDING"));
        if (pendingList.isEmpty()) {
            return 0;
        }
        Long operatorId = SecurityUtil.getCurrentUserId();
        for (EntityExtractPending pending : pendingList) {
            if ("CONFIRM".equalsIgnoreCase(action)) {
                writeToEntityTable(pending);
                updateStatus(pending.getId(), "CONFIRMED", operatorId);
            } else {
                updateStatus(pending.getId(), "REJECTED", operatorId);
            }
        }
        return pendingList.size();
    }

    private void writeToEntityTable(EntityExtractPending pending) {
        switch (pending.getEntityType()) {
            case "ingredient" -> {
                if (ingredientMapper.selectCount(new LambdaQueryWrapper<BeautyIngredient>()
                        .eq(BeautyIngredient::getName, pending.getEntityName())) == 0) {
                    BeautyIngredient ingredient = new BeautyIngredient();
                    ingredient.setName(pending.getEntityName());
                    ingredient.setSource("auto_extract");
                    ingredient.setConfirmed(1);
                    ingredientMapper.insert(ingredient);
                }
            }
            case "effect" -> {
                if (effectMapper.selectCount(new LambdaQueryWrapper<BeautyEffect>()
                        .eq(BeautyEffect::getName, pending.getEntityName())) == 0) {
                    BeautyEffect effect = new BeautyEffect();
                    effect.setName(pending.getEntityName());
                    effectMapper.insert(effect);
                }
            }
            case "product" -> {
                if (productMapper.selectCount(new LambdaQueryWrapper<BeautyProduct>()
                        .eq(BeautyProduct::getName, pending.getEntityName())) == 0) {
                    BeautyProduct product = new BeautyProduct();
                    product.setName(pending.getEntityName());
                    product.setConfirmed(1);
                    productMapper.insert(product);
                }
            }
            default -> {
            }
        }
    }

    private void updateStatus(Long id, String status, Long operatorId) {
        pendingMapper.update(null, new LambdaUpdateWrapper<EntityExtractPending>()
                .eq(EntityExtractPending::getId, id)
                .set(EntityExtractPending::getStatus, status)
                .set(EntityExtractPending::getConfirmedBy, operatorId)
                .set(EntityExtractPending::getConfirmedAt, LocalDateTime.now()));
    }
}
