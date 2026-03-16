package com.beauty.knowledge.module.file.service;

import com.beauty.knowledge.module.file.domain.entity.ProcessTask;
import com.beauty.knowledge.module.file.domain.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadVO upload(MultipartFile file, Long knowledgeId, Long categoryId, String fileType);

    ProcessTask getTask(Long taskId);

    void retry(Long taskId);

    void remove(Long fileId);
}
