package com.beauty.knowledge.module.entity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import com.beauty.knowledge.module.entity.domain.entity.BeautyEffect;
import com.beauty.knowledge.module.entity.mapper.BeautyEffectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EffectService {

    private final BeautyEffectMapper effectMapper;

    public EffectService(BeautyEffectMapper effectMapper) {
        this.effectMapper = effectMapper;
    }

    public List<BeautyEffect> list(String keyword) {
        return effectMapper.selectList(new LambdaQueryWrapper<BeautyEffect>()
                .like(keyword != null && !keyword.isBlank(), BeautyEffect::getName, keyword)
                .orderByDesc(BeautyEffect::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BeautyEffect effect) {
        Long count = effectMapper.selectCount(new LambdaQueryWrapper<BeautyEffect>()
                .eq(BeautyEffect::getName, effect.getName()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ENTITY_NAME_EXISTS);
        }
        effectMapper.insert(effect);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BeautyEffect effect) {
        effect.setId(id);
        effectMapper.updateById(effect);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        effectMapper.deleteById(id);
    }
}
