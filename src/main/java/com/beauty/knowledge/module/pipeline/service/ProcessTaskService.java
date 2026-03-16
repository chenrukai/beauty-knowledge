package com.beauty.knowledge.module.pipeline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import com.beauty.knowledge.module.file.domain.entity.ProcessTask;
import com.beauty.knowledge.module.file.mapper.ProcessTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcessTaskService {

    private final ProcessTaskMapper processTaskMapper;

    public ProcessTaskService(ProcessTaskMapper processTaskMapper) {
        this.processTaskMapper = processTaskMapper;
    }

    public ProcessTask getByFileId(Long fileId) {
        LambdaQueryWrapper<ProcessTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessTask::getFileId, fileId).orderByDesc(ProcessTask::getId);
        List<ProcessTask> tasks = processTaskMapper.selectList(wrapper);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markProcessing(Long fileId) {
        ProcessTask task = getByFileId(fileId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Task not found");
        }
        processTaskMapper.update(null, new LambdaUpdateWrapper<ProcessTask>()
                .eq(ProcessTask::getFileId, fileId)
                .set(ProcessTask::getStatus, "PROCESSING")
                .set(ProcessTask::getProgress, 10)
                .set(ProcessTask::getStartedAt, LocalDateTime.now())
                .set(ProcessTask::getResultMsg, null));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long fileId, int progress) {
        processTaskMapper.update(null, new LambdaUpdateWrapper<ProcessTask>()
                .eq(ProcessTask::getFileId, fileId)
                .set(ProcessTask::getProgress, Math.max(0, Math.min(progress, 100))));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(Long fileId) {
        processTaskMapper.update(null, new LambdaUpdateWrapper<ProcessTask>()
                .eq(ProcessTask::getFileId, fileId)
                .set(ProcessTask::getStatus, "SUCCESS")
                .set(ProcessTask::getProgress, 100)
                .set(ProcessTask::getFinishedAt, LocalDateTime.now())
                .set(ProcessTask::getResultMsg, null));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long fileId, String reason) {
        ProcessTask task = getByFileId(fileId);
        int retry = task == null || task.getRetryCount() == null ? 0 : task.getRetryCount();
        processTaskMapper.update(null, new LambdaUpdateWrapper<ProcessTask>()
                .eq(ProcessTask::getFileId, fileId)
                .set(ProcessTask::getStatus, "FAILED")
                .set(ProcessTask::getFinishedAt, LocalDateTime.now())
                .set(ProcessTask::getResultMsg, reason)
                .set(ProcessTask::getRetryCount, retry + 1));
    }
}
