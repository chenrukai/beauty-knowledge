package com.beauty.knowledge.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.PageResult;
import com.beauty.knowledge.common.result.ResultCode;
import com.beauty.knowledge.common.util.SecurityUtil;
import com.beauty.knowledge.infrastructure.vector.MilvusVectorStore;
import com.beauty.knowledge.module.file.domain.entity.KbFile;
import com.beauty.knowledge.module.file.mapper.KbFileMapper;
import com.beauty.knowledge.module.knowledge.domain.dto.KnowledgePageDTO;
import com.beauty.knowledge.module.knowledge.domain.dto.KnowledgeSaveDTO;
import com.beauty.knowledge.module.knowledge.domain.entity.KbChunk;
import com.beauty.knowledge.module.knowledge.domain.entity.KbKnowledge;
import com.beauty.knowledge.module.knowledge.domain.vo.KnowledgeDetailVO;
import com.beauty.knowledge.module.knowledge.mapper.KbChunkMapper;
import com.beauty.knowledge.module.knowledge.mapper.KbKnowledgeMapper;
import com.beauty.knowledge.module.knowledge.service.KnowledgeService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;

    private final KbKnowledgeMapper kbKnowledgeMapper;
    private final KbFileMapper kbFileMapper;
    private final KbChunkMapper kbChunkMapper;
    private final MilvusVectorStore milvusVectorStore;
    private final MinioClient minioClient;

    @Value("${beauty.minio.bucket-name}")
    private String minioBucketName;

    public KnowledgeServiceImpl(KbKnowledgeMapper kbKnowledgeMapper,
                                KbFileMapper kbFileMapper,
                                KbChunkMapper kbChunkMapper,
                                MilvusVectorStore milvusVectorStore,
                                MinioClient minioClient) {
        this.kbKnowledgeMapper = kbKnowledgeMapper;
        this.kbFileMapper = kbFileMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.milvusVectorStore = milvusVectorStore;
        this.minioClient = minioClient;
    }

    @Override
    public PageResult<KbKnowledge> page(KnowledgePageDTO dto) {
        long current = dto.getPageNum() == null || dto.getPageNum() <= 0 ? DEFAULT_PAGE_NUM : dto.getPageNum();
        long size = dto.getPageSize() == null || dto.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : dto.getPageSize();
        Page<KbKnowledge> page = new Page<>(current, size);

        LambdaQueryWrapper<KbKnowledge> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(dto.getKeyword()), KbKnowledge::getTitle, dto.getKeyword())
                .eq(dto.getCategoryId() != null, KbKnowledge::getCategoryId, dto.getCategoryId())
                .eq(dto.getStatus() != null, KbKnowledge::getStatus, dto.getStatus())
                .eq(StringUtils.hasText(dto.getType()), KbKnowledge::getType, dto.getType())
                .orderByDesc(KbKnowledge::getUpdatedAt)
                .orderByDesc(KbKnowledge::getId);
        Page<KbKnowledge> result = kbKnowledgeMapper.selectPage(page, queryWrapper);
        return PageResult.of(result);
    }

    @Override
    public KnowledgeDetailVO getById(Long id) {
        KbKnowledge kbKnowledge = kbKnowledgeMapper.selectById(id);
        if (kbKnowledge == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }

        LambdaQueryWrapper<KbFile> fileQueryWrapper = new LambdaQueryWrapper<>();
        fileQueryWrapper.eq(KbFile::getKnowledgeId, id).orderByDesc(KbFile::getCreatedAt);
        List<KbFile> files = kbFileMapper.selectList(fileQueryWrapper);

        return KnowledgeDetailVO.builder()
                .id(kbKnowledge.getId())
                .title(kbKnowledge.getTitle())
                .content(kbKnowledge.getContent())
                .summary(kbKnowledge.getSummary())
                .categoryId(kbKnowledge.getCategoryId())
                .type(kbKnowledge.getType())
                .coverUrl(kbKnowledge.getCoverUrl())
                .status(kbKnowledge.getStatus())
                .viewCount(kbKnowledge.getViewCount())
                .authorId(kbKnowledge.getAuthorId())
                .createdAt(kbKnowledge.getCreatedAt())
                .updatedAt(kbKnowledge.getUpdatedAt())
                .files(files)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(KnowledgeSaveDTO dto) {
        KbKnowledge kbKnowledge = new KbKnowledge();
        kbKnowledge.setTitle(dto.getTitle());
        kbKnowledge.setSummary(dto.getSummary());
        kbKnowledge.setCategoryId(dto.getCategoryId());
        kbKnowledge.setType(dto.getType());
        kbKnowledge.setContent(dto.getContent());
        kbKnowledge.setStatus(dto.getStatus());
        kbKnowledge.setViewCount(0);
        kbKnowledge.setAuthorId(SecurityUtil.getCurrentUserId());
        kbKnowledgeMapper.insert(kbKnowledge);
        log.info("Knowledge created, id={}, title={}", kbKnowledge.getId(), kbKnowledge.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, KnowledgeSaveDTO dto) {
        KbKnowledge existing = kbKnowledgeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }

        existing.setTitle(dto.getTitle());
        existing.setSummary(dto.getSummary());
        existing.setCategoryId(dto.getCategoryId());
        existing.setType(dto.getType());
        existing.setContent(dto.getContent());
        existing.setStatus(dto.getStatus());
        kbKnowledgeMapper.updateById(existing);
        log.info("Knowledge updated, id={}, title={}", existing.getId(), existing.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        KbKnowledge existing = kbKnowledgeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }

        LambdaQueryWrapper<KbFile> fileQueryWrapper = new LambdaQueryWrapper<>();
        fileQueryWrapper.eq(KbFile::getKnowledgeId, id);
        List<KbFile> files = kbFileMapper.selectList(fileQueryWrapper);

        LambdaQueryWrapper<KbChunk> chunkQueryWrapper = new LambdaQueryWrapper<>();
        chunkQueryWrapper.eq(KbChunk::getKnowledgeId, id);
        List<KbChunk> chunks = kbChunkMapper.selectList(chunkQueryWrapper);
        List<Long> chunkIds = chunks.stream()
                .map(KbChunk::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        kbChunkMapper.delete(chunkQueryWrapper);
        milvusVectorStore.deleteByChunkIds(chunkIds);

        for (KbFile file : files) {
            if (StringUtils.hasText(file.getMinioPath())) {
                deleteMinioObject(file.getMinioPath());
            }
        }

        LambdaQueryWrapper<KbFile> deleteFileWrapper = new LambdaQueryWrapper<>();
        deleteFileWrapper.eq(KbFile::getKnowledgeId, id);
        kbFileMapper.delete(deleteFileWrapper);

        kbKnowledgeMapper.deleteById(id);
        log.info("Knowledge removed, id={}, chunks={}, files={}", id, chunkIds.size(), files.size());
    }

    @Override
    public PageResult<KbKnowledge> search(String keyword, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum <= 0 ? DEFAULT_PAGE_NUM : pageNum;
        long size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        if (!StringUtils.hasText(keyword)) {
            Page<KbKnowledge> page = new Page<>(current, size);
            LambdaQueryWrapper<KbKnowledge> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(KbKnowledge::getStatus, 1)
                    .orderByDesc(KbKnowledge::getUpdatedAt)
                    .orderByDesc(KbKnowledge::getId);
            Page<KbKnowledge> result = kbKnowledgeMapper.selectPage(page, queryWrapper);
            return PageResult.of(result);
        }

        long offset = (current - 1) * size;
        List<KbKnowledge> records = kbKnowledgeMapper.fullTextSearch(keyword, offset, size);
        Long total = kbKnowledgeMapper.fullTextSearchCount(keyword);
        long safeTotal = total == null ? 0L : total;
        long pages = safeTotal == 0 ? 0 : (safeTotal + size - 1) / size;
        return new PageResult<>(records == null ? new ArrayList<>() : records, safeTotal, current, size, pages);
    }

    private void deleteMinioObject(String minioPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioBucketName)
                            .object(normalizeObjectName(minioPath))
                            .build()
            );
        } catch (Exception ex) {
            log.error("Delete minio object failed, path={}", minioPath, ex);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Delete MinIO object failed");
        }
    }

    private String normalizeObjectName(String minioPath) {
        String object = minioPath.trim();
        String prefix = minioBucketName + "/";
        if (object.startsWith(prefix)) {
            return object.substring(prefix.length());
        }
        if (object.startsWith("/")) {
            return object.substring(1);
        }
        return object;
    }
}
