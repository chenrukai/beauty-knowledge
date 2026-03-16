package com.beauty.knowledge.module.entity.controller;

import com.beauty.knowledge.common.result.Result;
import com.beauty.knowledge.module.entity.domain.entity.BeautyEffect;
import com.beauty.knowledge.module.entity.domain.entity.BeautyIngredient;
import com.beauty.knowledge.module.entity.service.RelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "实体关联管理")
@RestController
@RequestMapping("/api/entity/relation")
public class RelationController {

    private final RelationService relationService;

    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }

    @Operation(summary = "绑定成分-功效")
    @PostMapping("/ingredient-effect")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> bindIngredientEffect(@RequestBody BindIngredientEffectReq req) {
        relationService.bindIngredientEffect(req.getIngredientId(), req.getEffectId(), req.getStrength());
        return Result.success();
    }

    @Operation(summary = "解绑成分-功效")
    @DeleteMapping("/ingredient-effect/{ingredientId}/{effectId}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> unbindIngredientEffect(@PathVariable Long ingredientId, @PathVariable Long effectId) {
        relationService.unbindIngredientEffect(ingredientId, effectId);
        return Result.success();
    }

    @Operation(summary = "绑定产品-成分")
    @PostMapping("/product-ingredient")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> bindProductIngredient(@RequestBody BindProductIngredientReq req) {
        relationService.bindProductIngredient(
                req.getProductId(), req.getIngredientId(), req.getConcentration(), Boolean.TRUE.equals(req.getIsKey()));
        return Result.success();
    }

    @Operation(summary = "解绑产品-成分")
    @DeleteMapping("/product-ingredient/{productId}/{ingredientId}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> unbindProductIngredient(@PathVariable Long productId, @PathVariable Long ingredientId) {
        relationService.unbindProductIngredient(productId, ingredientId);
        return Result.success();
    }

    @Operation(summary = "查询成分关联功效")
    @GetMapping("/ingredient-effect/{ingredientId}")
    public Result<List<BeautyEffect>> getEffectsByIngredient(@PathVariable Long ingredientId) {
        return Result.success(relationService.getEffectsByIngredient(ingredientId));
    }

    @Operation(summary = "查询产品关联成分")
    @GetMapping("/product-ingredient/{productId}")
    public Result<List<BeautyIngredient>> getIngredientsByProduct(@PathVariable Long productId) {
        return Result.success(relationService.getIngredientsByProduct(productId));
    }

    @Data
    public static class BindIngredientEffectReq {
        private Long ingredientId;
        private Long effectId;
        private String strength;
    }

    @Data
    public static class BindProductIngredientReq {
        private Long productId;
        private Long ingredientId;
        private String concentration;
        private Boolean isKey;
    }
}
