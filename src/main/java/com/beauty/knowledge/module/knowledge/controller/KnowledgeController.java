package com.beauty.knowledge.module.knowledge.controller;

import com.beauty.knowledge.common.result.PageResult;
import com.beauty.knowledge.common.result.Result;
import com.beauty.knowledge.module.knowledge.domain.dto.KnowledgePageDTO;
import com.beauty.knowledge.module.knowledge.domain.dto.KnowledgeSaveDTO;
import com.beauty.knowledge.module.knowledge.domain.entity.KbKnowledge;
import com.beauty.knowledge.module.knowledge.domain.vo.KnowledgeDetailVO;
import com.beauty.knowledge.module.knowledge.service.KnowledgeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识内容管理")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Operation(summary = "分页查询知识")
    @GetMapping("/page")
    public Result<PageResult<KbKnowledge>> page(KnowledgePageDTO dto) {
        return Result.success(knowledgeService.page(dto));
    }

    @Operation(summary = "知识详情")
    @GetMapping("/{id}")
    public Result<KnowledgeDetailVO> getById(@PathVariable Long id) {
        return Result.success(knowledgeService.getById(id));
    }

    @Operation(summary = "新增知识")
    @PreAuthorize("hasRole('admin')")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody KnowledgeSaveDTO dto) {
        knowledgeService.save(dto);
        return Result.success();
    }

    @Operation(summary = "更新知识")
    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody KnowledgeSaveDTO dto) {
        knowledgeService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除知识")
    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        knowledgeService.remove(id);
        return Result.success();
    }

    @Operation(summary = "全文检索")
    @GetMapping("/search")
    public Result<PageResult<KbKnowledge>> search(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") Long pageNum,
                                                  @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(knowledgeService.search(keyword, pageNum, pageSize));
    }
}
