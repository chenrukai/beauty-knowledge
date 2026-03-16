package com.beauty.knowledge.module.pipeline.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.beauty.knowledge.common.constant.RabbitMQConstant;
import com.beauty.knowledge.infrastructure.ai.python.PythonAIClient;
import com.beauty.knowledge.infrastructure.storage.MinioStorageService;
import com.beauty.knowledge.infrastructure.vector.MilvusVectorStore;
import com.beauty.knowledge.module.entity.service.EntityExtractService;
import com.beauty.knowledge.module.file.domain.entity.KbFile;
import com.beauty.knowledge.module.file.mapper.KbFileMapper;
import com.beauty.knowledge.module.knowledge.domain.entity.KbChunk;
import com.beauty.knowledge.module.knowledge.mapper.KbChunkMapper;
import com.beauty.knowledge.module.pipeline.domain.dto.ChunkDTO;
import com.beauty.knowledge.module.pipeline.domain.mq.ProcessMessage;
import com.beauty.knowledge.module.pipeline.service.ChunkService;
import com.beauty.knowledge.module.pipeline.service.ProcessTaskService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KnowledgeProcessConsumer {

    private final ProcessTaskService processTaskService;
    private final ChunkService chunkService;
    private final KbChunkMapper kbChunkMapper;
    private final KbFileMapper kbFileMapper;
    private final PythonAIClient pythonAIClient;
    private final MilvusVectorStore milvusVectorStore;
    private final EntityExtractService entityExtractService;
    private final MinioStorageService minioStorageService;
    private final Tika tika = new Tika();

    public KnowledgeProcessConsumer(ProcessTaskService processTaskService,
                                    ChunkService chunkService,
                                    KbChunkMapper kbChunkMapper,
                                    KbFileMapper kbFileMapper,
                                    PythonAIClient pythonAIClient,
                                    MilvusVectorStore milvusVectorStore,
                                    EntityExtractService entityExtractService,
                                    MinioStorageService minioStorageService) {
        this.processTaskService = processTaskService;
        this.chunkService = chunkService;
        this.kbChunkMapper = kbChunkMapper;
        this.kbFileMapper = kbFileMapper;
        this.pythonAIClient = pythonAIClient;
        this.milvusVectorStore = milvusVectorStore;
        this.entityExtractService = entityExtractService;
        this.minioStorageService = minioStorageService;
    }

    @RabbitListener(queues = RabbitMQConstant.PROCESS_QUEUE)
    public void onMessage(ProcessMessage message, Channel channel, Message amqpMessage) throws Exception {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            processInternal(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Knowledge process failed, fileId={}", message.getFileId(), ex);
            processTaskService.markFailed(message.getFileId(), ex.getMessage());
            kbFileMapper.update(null, new LambdaUpdateWrapper<KbFile>()
                    .eq(KbFile::getId, message.getFileId())
                    .set(KbFile::getProcessStatus, "FAILED"));
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void processInternal(ProcessMessage msg) throws Exception {
        Long fileId = msg.getFileId();
        processTaskService.markProcessing(fileId);
        kbFileMapper.update(null, new LambdaUpdateWrapper<KbFile>()
                .eq(KbFile::getId, fileId)
                .set(KbFile::getProcessStatus, "PROCESSING"));

        processTaskService.updateProgress(fileId, 20);
        byte[] fileBytes = minioStorageService.download(msg.getMinioPath());
        String text = tika.parseToString(new ByteArrayInputStream(fileBytes));
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Extracted text is empty");
        }

        processTaskService.updateProgress(fileId, 40);
        List<ChunkDTO> chunks = chunkService.split(text, 1);
        List<KbChunk> entities = new ArrayList<>();
        for (ChunkDTO chunk : chunks) {
            KbChunk entity = new KbChunk();
            entity.setFileId(fileId);
            entity.setKnowledgeId(msg.getKnowledgeId());
            entity.setChunkIndex(chunk.getChunkIndex());
            entity.setContent(chunk.getContent());
            entity.setPage(chunk.getPage());
            entity.setCharCount(chunk.getCharCount());
            kbChunkMapper.insert(entity);
            entities.add(entity);
        }

        processTaskService.updateProgress(fileId, 65);
        List<String> contents = entities.stream().map(KbChunk::getContent).collect(Collectors.toList());
        List<Long> chunkIds = entities.stream().map(KbChunk::getId).collect(Collectors.toList());
        List<float[]> vectors = pythonAIClient.embed(contents);
        milvusVectorStore.batchInsert(chunkIds, vectors, fileId, msg.getCategoryId(), contents);

        processTaskService.updateProgress(fileId, 85);
        entityExtractService.extractAsync(fileId, text);

        kbFileMapper.update(null, new LambdaUpdateWrapper<KbFile>()
                .eq(KbFile::getId, fileId)
                .set(KbFile::getProcessStatus, "SUCCESS"));
        processTaskService.markSuccess(fileId);
    }
}
