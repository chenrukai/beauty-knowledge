package com.beauty.knowledge.infrastructure.storage;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${beauty.minio.bucket-name}")
    private String bucketName;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String buildPath(String fileType, String originalFilename) {
        String safeType = StringUtils.hasText(fileType) ? fileType.trim().toLowerCase() : "unknown";
        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        LocalDate now = LocalDate.now();
        return "%s/%d/%02d/%02d/%s%s".formatted(
                safeType,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                ext
        );
    }

    public void upload(byte[] bytes, String minioPath, String contentType) {
        try {
            byte[] safeBytes = bytes == null ? new byte[0] : bytes;
            String safeContentType = StringUtils.hasText(contentType)
                    ? contentType
                    : "application/octet-stream";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(minioPath)
                            .stream(new ByteArrayInputStream(safeBytes), safeBytes.length, -1)
                            .contentType(safeContentType)
                            .build()
            );
            log.info("Upload file to MinIO success, path={}", minioPath);
        } catch (Exception ex) {
            log.error("Upload file to MinIO failed, path={}", minioPath, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Upload file to MinIO failed");
        }
    }

    public void delete(String minioPath) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(normalizeObjectName(minioPath))
                            .build()
            );
        } catch (Exception ex) {
            log.error("Delete MinIO object failed, path={}", minioPath, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Delete MinIO object failed");
        }
    }

    private String normalizeObjectName(String minioPath) {
        if (!StringUtils.hasText(minioPath)) {
            return "";
        }
        String object = minioPath.trim();
        String prefix = bucketName + "/";
        if (object.startsWith(prefix)) {
            return object.substring(prefix.length());
        }
        if (object.startsWith("/")) {
            return object.substring(1);
        }
        return object;
    }
}
