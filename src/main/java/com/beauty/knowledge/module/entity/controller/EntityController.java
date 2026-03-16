package com.beauty.knowledge.module.entity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beauty.knowledge.common.result.Result;
import com.beauty.knowledge.common.result.PageResult;
import com.beauty.knowledge.module.entity.domain.dto.PendingPageDTO;
import com.beauty.knowledge.module.entity.domain.entity.BeautyEffect;
import com.beauty.knowledge.module.entity.domain.entity.BeautyIngredient;
import com.beauty.knowledge.module.entity.domain.entity.BeautyProduct;
import com.beauty.knowledge.module.entity.domain.entity.EntityExtractPending;
import com.beauty.knowledge.module.entity.mapper.EntityExtractPendingMapper;
import com.beauty.knowledge.module.entity.service.EffectService;
import com.beauty.knowledge.module.entity.service.EntityConfirmService;
import com.beauty.knowledge.module.entity.service.IngredientService;
import com.beauty.knowledge.module.entity.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "实体管理")
@RestController
@RequestMapping("/api/entity")
public class EntityController {

    private final IngredientService ingredientService;
    private final EffectService effectService;
    private final ProductService productService;
    private final EntityExtractPendingMapper pendingMapper;
    private final EntityConfirmService entityConfirmService;

    public EntityController(IngredientService ingredientService,
                            EffectService effectService,
                            ProductService productService,
                            EntityExtractPendingMapper pendingMapper,
                            EntityConfirmService entityConfirmService) {
        this.ingredientService = ingredientService;
        this.effectService = effectService;
        this.productService = productService;
        this.pendingMapper = pendingMapper;
        this.entityConfirmService = entityConfirmService;
    }

    @Operation(summary = "成分列表")
    @GetMapping("/ingredient")
    public Result<List<BeautyIngredient>> ingredients(@RequestParam(required = false) String keyword) {
        return Result.success(ingredientService.list(keyword));
    }

    @Operation(summary = "新增成分")
    @PostMapping("/ingredient")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> saveIngredient(@RequestBody BeautyIngredient ingredient) {
        ingredientService.save(ingredient);
        return Result.success();
    }

    @Operation(summary = "更新成分")
    @PutMapping("/ingredient/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updateIngredient(@PathVariable Long id, @RequestBody BeautyIngredient ingredient) {
        ingredientService.update(id, ingredient);
        return Result.success();
    }

    @Operation(summary = "删除成分")
    @DeleteMapping("/ingredient/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> removeIngredient(@PathVariable Long id) {
        ingredientService.remove(id);
        return Result.success();
    }

    @Operation(summary = "功效列表")
    @GetMapping("/effect")
    public Result<List<BeautyEffect>> effects(@RequestParam(required = false) String keyword) {
        return Result.success(effectService.list(keyword));
    }

    @Operation(summary = "新增功效")
    @PostMapping("/effect")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> saveEffect(@RequestBody BeautyEffect effect) {
        effectService.save(effect);
        return Result.success();
    }

    @Operation(summary = "更新功效")
    @PutMapping("/effect/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updateEffect(@PathVariable Long id, @RequestBody BeautyEffect effect) {
        effectService.update(id, effect);
        return Result.success();
    }

    @Operation(summary = "删除功效")
    @DeleteMapping("/effect/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> removeEffect(@PathVariable Long id) {
        effectService.remove(id);
        return Result.success();
    }

    @Operation(summary = "产品列表")
    @GetMapping("/product")
    public Result<List<BeautyProduct>> products(@RequestParam(required = false) String keyword) {
        return Result.success(productService.list(keyword));
    }

    @Operation(summary = "新增产品")
    @PostMapping("/product")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> saveProduct(@RequestBody BeautyProduct product) {
        productService.save(product);
        return Result.success();
    }

    @Operation(summary = "更新产品")
    @PutMapping("/product/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody BeautyProduct product) {
        productService.update(id, product);
        return Result.success();
    }

    @Operation(summary = "删除产品")
    @DeleteMapping("/product/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> removeProduct(@PathVariable Long id) {
        productService.remove(id);
        return Result.success();
    }

    @Operation(summary = "待确认实体列表")
    @GetMapping("/pending")
    public Result<List<EntityExtractPending>> pending(@RequestParam(required = false) String entityType,
                                                      @RequestParam(required = false) String extractMethod) {
        LambdaQueryWrapper<EntityExtractPending> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(entityType != null && !entityType.isBlank(), EntityExtractPending::getEntityType, entityType)
                .eq(extractMethod != null && !extractMethod.isBlank(), EntityExtractPending::getExtractMethod, extractMethod)
                .eq(EntityExtractPending::getStatus, "PENDING")
                .orderByDesc(EntityExtractPending::getId);
        return Result.success(pendingMapper.selectList(wrapper));
    }

    @Operation(summary = "待确认实体分页")
    @GetMapping("/pending/page")
    public Result<PageResult<EntityExtractPending>> pendingPage(PendingPageDTO dto) {
        long pageNum = dto.getPageNum() == null || dto.getPageNum() <= 0 ? 1L : dto.getPageNum();
        long pageSize = dto.getPageSize() == null || dto.getPageSize() <= 0 ? 20L : dto.getPageSize();
        Page<EntityExtractPending> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EntityExtractPending> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getEntityType() != null && !dto.getEntityType().isBlank(), EntityExtractPending::getEntityType, dto.getEntityType())
                .eq(dto.getExtractMethod() != null && !dto.getExtractMethod().isBlank(), EntityExtractPending::getExtractMethod, dto.getExtractMethod())
                .eq(EntityExtractPending::getStatus, "PENDING")
                .orderByDesc(EntityExtractPending::getCreatedAt)
                .orderByDesc(EntityExtractPending::getId);
        return Result.success(PageResult.of(pendingMapper.selectPage(page, wrapper)));
    }

    @Operation(summary = "待确认实体数量")
    @GetMapping("/pending/count")
    public Result<Long> pendingCount() {
        Long count = pendingMapper.selectCount(new LambdaQueryWrapper<EntityExtractPending>()
                .eq(EntityExtractPending::getStatus, "PENDING"));
        return Result.success(count == null ? 0L : count);
    }

    @Operation(summary = "批量确认/拒绝")
    @PostMapping("/pending/confirm")
    @PreAuthorize("hasRole('admin')")
    public Result<Integer> confirm(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) req.get("ids");
        String action = (String) req.getOrDefault("action", "CONFIRM");
        List<Long> ids = rawIds == null ? List.of() : rawIds.stream().map(Integer::longValue).toList();
        return Result.success(entityConfirmService.confirm(ids, action));
    }
}
