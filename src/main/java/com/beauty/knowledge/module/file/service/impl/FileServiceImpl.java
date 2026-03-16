package com.beauty.knowledge.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beauty.knowledge.common.constant.RabbitMQConstant;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import com.beauty.knowledge.common.result.PageResult;
import com.beauty.knowledge.common.result.ResultCode;
import com.beauty.knowledge.common.util.FileHashUtil;
import com.beauty.knowledge.common.util.SecurityUtil;
import com.beauty.knowledge.infrastructure.storage.MinioStorageService;
import com.beauty.knowledge.infrastructure.vector.MilvusVectorStore;
import com.beauty.knowledge.module.file.domain.dto.TaskPageDTO;
import com.beauty.knowledge.module.file.domain.entity.KbFile;
import com.beauty.knowledge.module.file.domain.entity.ProcessTask;
import com.beauty.knowledge.module.file.domain.vo.FileUploadVO;
import com.beauty.knowledge.module.file.mapper.KbFileMapper;
import com.beauty.knowledge.module.file.mapper.ProcessTaskMapper;
import com.beauty.knowledge.module.file.service.FileService;
import com.beauty.knowledge.module.knowledge.domain.entity.KbChunk;
import com.beauty.knowledge.module.knowledge.domain.entity.KbKnowledge;
import com.beauty.knowledge.module.knowledge.mapper.KbChunkMapper;
import com.beauty.knowledge.module.knowledge.mapper.KbKnowledgeMapper;
import com.beauty.knowledge.module.pipeline.domain.mq.ProcessMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_FAILED = "FAILED";

    private final KbFileMapper kbFileMapper;
    private final ProcessTaskMapper processTaskMapper;
    private final MinioStorageService minioStorageService;
    private final RabbitTemplate rabbitTemplate;
    private final KbKnowledgeMapper kbKnowledgeMapper;
    private final KbChunkMapper kbChunkMapper;
    private final MilvusVectorStore milvusVectorStore;

    @Value("${beauty.pipeline.max-retry:3}")
    private Integer maxRetry;

    @Value("${beauty.pipeline.task-type:KNOWLEDGE_PROCESS}")
    private String taskType;

    public FileServiceImpl(KbFileMapper kbFileMapper,
                           ProcessTaskMapper processTaskMapper,
                           MinioStorageService minioStorageService,
                           RabbitTemplate rabbitTemplate,
                           KbKnowledgeMapper kbKnowledgeMapper,
                           KbChunkMapper kbChunkMapper,
                           MilvusVectorStore milvusVectorStore) {
        this.kbFileMapper = kbFileMapper;
        this.processTaskMapper = processTaskMapper;
        this.minioStorageService = minioStorageService;
        this.rabbitTemplate = rabbitTemplate;
        this.kbKnowledgeMapper = kbKnowledgeMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.milvusVectorStore = milvusVectorStore;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO upload(MultipartFile file, Long knowledgeId,
                               Long categoryId, String fileType) {
        // Step 1: calculate SHA-256 fileHash
        String fileHash = FileHashUtil.sha256(file);

        // Step 2: deduplicate
        KbFile existing = kbFileMapper.selectOne(
                new LambdaQueryWrapper<KbFile>().eq(KbFile::getFileHash, fileHash));
        if (existing != null) {
            throw new BusinessException(ErrorCode.FILE_HASH_DUPLICATE);
        }

        // Step 3: upload to MinIO
        String minioPath = minioStorageService.buildPath(fileType, file.getOriginalFilename());
        try {
            minioPath = minioStorageService.upload(file.getBytes(), minioPath, file.getContentType());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Read upload file failed");
        }

        // Step 4: insert kb_file
        KbFile kbFile = new KbFile();
        kbFile.setKnowledgeId(knowledgeId);
        kbFile.setOriginalName(file.getOriginalFilename());
        kbFile.setFileType(fileType);
        kbFile.setFileSize(file.getSize());
        kbFile.setMinioPath(minioPath);
        kbFile.setFileHash(fileHash);
        kbFile.setVersion(1);
        kbFile.setProcessStatus(STATUS_PENDING);
        kbFile.setUploadedBy(SecurityUtil.getCurrentUserId());
        kbFileMapper.insert(kbFile);

        // Step 5: insert process_task
        ProcessTask task = new ProcessTask();
        task.setFileId(kbFile.getId());
        task.setTaskType(taskType);
        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setResultMsg(null);
        task.setRetryCount(0);
        task.setMaxRetry(maxRetry);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        processTaskMapper.insert(task);

        // Step 6: send MQ after transaction commit
        Long finalFileId = kbFile.getId();
        Long finalTaskId = task.getId();
        String finalMinioPath = minioPath;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ProcessMessage msg = ProcessMessage.builder()
                                .fileId(finalFileId)
                                .fileType(fileType)
                                .minioPath(finalMinioPath)
                                .categoryId(categoryId)
                                .knowledgeId(knowledgeId)
                                .build();
                        rabbitTemplate.convertAndSend(
                                RabbitMQConstant.PROCESS_EXCHANGE,
                                RabbitMQConstant.PROCESS_ROUTING_KEY, msg);
                    }
                }
        );

        return FileUploadVO.builder()
                .fileId(kbFile.getId())
                .taskId(finalTaskId)
                .minioPath(finalMinioPath)
                .processStatus(STATUS_PENDING)
                .build();
    }

    @Override
    public ProcessTask getTask(Long taskId) {
        ProcessTask task = processTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Task not found");
        }
        return task;
    }

    @Override
    public PageResult<ProcessTask> pageTasks(TaskPageDTO dto) {
        long pageNum = dto.getPageNum() == null || dto.getPageNum() <= 0 ? 1L : dto.getPageNum();
        long pageSize = dto.getPageSize() == null || dto.getPageSize() <= 0 ? 20L : dto.getPageSize();
        Page<ProcessTask> page = new Page<>(pageNum, pageSize);

        LocalDateTime start = parseStart(dto.getStartDate());
        LocalDateTime end = parseEnd(dto.getEndDate());
        LambdaQueryWrapper<ProcessTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dto.getStatus()), ProcessTask::getStatus, dto.getStatus())
                .ge(start != null, ProcessTask::getCreatedAt, start)
                .le(end != null, ProcessTask::getCreatedAt, end)
                .orderByDesc(ProcessTask::getCreatedAt)
                .orderByDesc(ProcessTask::getId);

        Page<ProcessTask> result = processTaskMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retry(Long taskId) {
        ProcessTask task = processTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Task not found");
        }
        if (!STATUS_FAILED.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.FILE_PROCESSING);
        }

        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setResultMsg(null);
        task.setRetryCount(0);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        processTaskMapper.updateById(task);

        KbFile kbFile = kbFileMapper.selectById(task.getFileId());
        if (kbFile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "File not found");
        }
        Long knowledgeId = kbFile.getKnowledgeId();
        Long categoryId = null;
        if (knowledgeId != null) {
            KbKnowledge knowledge = kbKnowledgeMapper.selectById(knowledgeId);
            if (knowledge != null) {
                categoryId = knowledge.getCategoryId();
            }
        }

        Long finalCategoryId = categoryId;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ProcessMessage msg = ProcessMessage.builder()
                                .fileId(kbFile.getId())
                                .fileType(kbFile.getFileType())
                                .minioPath(kbFile.getMinioPath())
                                .categoryId(finalCategoryId)
                                .knowledgeId(kbFile.getKnowledgeId())
                                .retryCount(0)
                                .build();
                        rabbitTemplate.convertAndSend(
                                RabbitMQConstant.PROCESS_EXCHANGE,
                                RabbitMQConstant.PROCESS_ROUTING_KEY, msg);
                    }
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long fileId) {
        KbFile kbFile = kbFileMapper.selectById(fileId);
        if (kbFile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "File not found");
        }

        LambdaQueryWrapper<KbChunk> chunkQueryWrapper = new LambdaQueryWrapper<>();
        chunkQueryWrapper.eq(KbChunk::getFileId, fileId);
        List<KbChunk> chunks = kbChunkMapper.selectList(chunkQueryWrapper);
        List<Long> chunkIds = chunks.stream()
                .map(KbChunk::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        kbChunkMapper.delete(chunkQueryWrapper);
        milvusVectorStore.deleteByChunkIds(chunkIds);

        if (StringUtils.hasText(kbFile.getMinioPath())) {
            minioStorageService.delete(kbFile.getMinioPath());
        }

        LambdaQueryWrapper<ProcessTask> taskQueryWrapper = new LambdaQueryWrapper<>();
        taskQueryWrapper.eq(ProcessTask::getFileId, fileId);
        processTaskMapper.delete(taskQueryWrapper);
        kbFileMapper.deleteById(fileId);
        log.info("File removed, fileId={}, chunkCount={}", fileId, chunkIds.size());
    }

    private LocalDateTime parseStart(String date) {
        try {
            return StringUtils.hasText(date) ? LocalDate.parse(date).atStartOfDay() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseEnd(String date) {
        try {
            return StringUtils.hasText(date) ? LocalDate.parse(date).atTime(23, 59, 59) : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
