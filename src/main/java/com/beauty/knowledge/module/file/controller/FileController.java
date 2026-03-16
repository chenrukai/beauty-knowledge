package com.beauty.knowledge.module.file.controller;

import com.beauty.knowledge.common.result.Result;
import com.beauty.knowledge.common.result.PageResult;
import com.beauty.knowledge.module.file.domain.dto.TaskPageDTO;
import com.beauty.knowledge.module.file.domain.entity.ProcessTask;
import com.beauty.knowledge.module.file.domain.vo.FileUploadVO;
import com.beauty.knowledge.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传管理")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) Long knowledgeId,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam String fileType) {
        return Result.success(fileService.upload(file, knowledgeId, categoryId, fileType));
    }

    @Operation(summary = "查询任务状态")
    @GetMapping("/task/{taskId}")
    public Result<ProcessTask> getTask(@PathVariable Long taskId) {
        return Result.success(fileService.getTask(taskId));
    }

    @Operation(summary = "任务分页列表")
    @GetMapping("/task/list")
    public Result<PageResult<ProcessTask>> taskList(TaskPageDTO dto) {
        return Result.success(fileService.pageTasks(dto));
    }

    @Operation(summary = "重试任务")
    @PostMapping("/task/{taskId}/retry")
    public Result<Void> retry(@PathVariable Long taskId) {
        fileService.retry(taskId);
        return Result.success();
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{fileId}")
    public Result<Void> remove(@PathVariable Long fileId) {
        fileService.remove(fileId);
        return Result.success();
    }
}
