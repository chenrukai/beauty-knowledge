package com.beauty.knowledge.infrastructure.storage;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.exception.ErrorCode;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${beauty.minio.bucket-name}")
    private String bucketName;

    @Value("${beauty.minio.endpoint}")
    private String endpoint;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String upload(byte[] bytes, String path, String contentType) {
        String relativePath = normalizePath(path);
        try {
            byte[] safeBytes = bytes == null ? new byte[0] : bytes;
            String safeContentType = StringUtils.hasText(contentType)
                    ? contentType
                    : "application/octet-stream";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(relativePath)
                            .stream(new ByteArrayInputStream(safeBytes), safeBytes.length, -1)
                            .contentType(safeContentType)
                            .build()
            );
            return relativePath;
        } catch (Exception ex) {
            log.error("Upload file to MinIO failed, path={}", relativePath, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Upload file to MinIO failed");
        }
    }

    public byte[] download(String path) {
        String relativePath = normalizePath(path);
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(relativePath).build())) {
            return inputStream.readAllBytes();
        } catch (Exception ex) {
            log.error("Download file from MinIO failed, path={}", relativePath, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Download file from MinIO failed");
        }
    }

    public void delete(String path) {
        String relativePath = normalizePath(path);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(relativePath).build()
            );
        } catch (Exception ex) {
            log.error("Delete file from MinIO failed, path={}", relativePath, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Delete file from MinIO failed");
        }
    }

    public String buildPath(String fileType, String originalName) {
        String safeType = StringUtils.hasText(fileType) ? fileType.trim().toLowerCase() : "unknown";
        String ext = "";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
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

    public String getAccessUrl(String path) {
        String relativePath = normalizePath(path);
        String cleanEndpoint = endpoint == null ? "" : endpoint.replaceAll("/+$", "");
        return cleanEndpoint + "/" + bucketName + "/" + relativePath;
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String relativePath = path.trim();
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        String bucketPrefix = bucketName + "/";
        if (relativePath.startsWith(bucketPrefix)) {
            relativePath = relativePath.substring(bucketPrefix.length());
        }
        return relativePath;
    }
}
