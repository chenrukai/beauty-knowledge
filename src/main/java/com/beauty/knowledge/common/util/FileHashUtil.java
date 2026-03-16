package com.beauty.knowledge.common.util;

import com.beauty.knowledge.common.exception.BusinessException;
import com.beauty.knowledge.common.result.ResultCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileHashUtil {

    private FileHashUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String sha256(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "File must not be empty");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return sha256(inputStream);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to read file stream");
        }
    }

    public static String sha256(byte[] data) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Data must not be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "SHA-256 algorithm unavailable");
        }
    }

    public static String sha256(InputStream inputStream) {
        if (inputStream == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "InputStream must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to read input stream");
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "SHA-256 algorithm unavailable");
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
