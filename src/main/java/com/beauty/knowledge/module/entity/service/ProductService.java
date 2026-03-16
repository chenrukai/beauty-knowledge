package com.beauty.knowledge.module.entity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import com.beauty.knowledge.module.entity.domain.entity.BeautyProduct;
import com.beauty.knowledge.module.entity.mapper.BeautyProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final BeautyProductMapper productMapper;

    public ProductService(BeautyProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<BeautyProduct> list(String keyword) {
        return productMapper.selectList(new LambdaQueryWrapper<BeautyProduct>()
                .like(keyword != null && !keyword.isBlank(), BeautyProduct::getName, keyword)
                .orderByDesc(BeautyProduct::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BeautyProduct product) {
        Long count = productMapper.selectCount(new LambdaQueryWrapper<BeautyProduct>()
                .eq(BeautyProduct::getName, product.getName()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ENTITY_NAME_EXISTS);
        }
        productMapper.insert(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BeautyProduct product) {
        product.setId(id);
        productMapper.updateById(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        productMapper.deleteById(id);
    }
}
