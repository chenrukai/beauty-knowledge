package com.beauty.knowledge.module.category.controller;

import com.beauty.knowledge.common.result.Result;
import com.beauty.knowledge.module.category.domain.dto.CategorySaveDTO;
import com.beauty.knowledge.module.category.domain.vo.CategoryTreeVO;
import com.beauty.knowledge.module.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> getTree() {
        return Result.success(categoryService.getTree());
    }

    @Operation(summary = "新增分类")
    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody CategorySaveDTO dto) {
        categoryService.save(dto);
        return Result.success();
    }

    @Operation(summary = "更新分类")
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        categoryService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        categoryService.remove(id);
        return Result.success();
    }
}
