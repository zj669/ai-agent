package com.zj.aiagent.application.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文件验证工具类
 * 职责：验证上传文件的安全性和合法性
 */
@Slf4j
public class FileValidator {

    /**
     * 允许的文件类型白名单（MIME types）
     */
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",                                                      // PDF
            "application/msword",                                                   // DOC
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
            "text/plain",                                                           // TXT
            "text/markdown",                                                        // MD
            "text/csv",                                                             // CSV
            "application/vnd.ms-excel",                                             // XLS
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"    // XLSX
    );

    /**
     * 允许的文件扩展名白名单
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "txt", "md", "csv", "xls", "xlsx"
    );

    /**
     * 最大文件大小：50MB
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 路径遍历攻击检测正则
     */
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*[/\\\\]\\.\\.[\\\\/].*");

    /**
     * 验证上传文件
     *
     * @param file 上传的文件
     * @throws IllegalArgumentException 验证失败时抛出
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 1. 检查文件名安全性（防止路径遍历攻击）
        validateFilename(filename);

        // 2. 检查文件大小
        validateFileSize(file.getSize(), filename);

        // 3. 检查文件类型（扩展名 + MIME type）
        validateFileType(filename, file.getContentType());

        log.debug("文件验证通过: filename={}, size={}, contentType={}",
                filename, file.getSize(), file.getContentType());
    }

    /**
     * 验证文件名安全性
     * 防止路径遍历攻击（如 ../../etc/passwd）
     */
    private static void validateFilename(String filename) {
        // 检查路径遍历字符
        if (PATH_TRAVERSAL_PATTERN.matcher(filename).matches()) {
            log.warn("检测到路径遍历攻击尝试: filename={}", filename);
            throw new IllegalArgumentException("文件名包含非法字符");
        }

        // 检查文件名长度
        if (filename.length() > 255) {
            throw new IllegalArgumentException("文件名过长（最大255字符）");
        }

        // 检查是否包含特殊字符（可选，根据需求调整）
        if (filename.contains("\0")) {
            throw new IllegalArgumentException("文件名包含非法字符");
        }
    }

    /**
     * 验证文件大小
     */
    private static void validateFileSize(long fileSize, String filename) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小无效");
        }

        if (fileSize > MAX_FILE_SIZE) {
            log.warn("文件大小超过限制: filename={}, size={}, limit={}",
                    filename, fileSize, MAX_FILE_SIZE);
            throw new IllegalArgumentException(
                    String.format("文件大小超过限制（最大 %d MB）", MAX_FILE_SIZE / 1024 / 1024));
        }
    }

    /**
     * 验证文件类型
     * 同时检查扩展名和 MIME type
     */
    private static void validateFileType(String filename, String contentType) {
        // 1. 检查扩展名
        String extension = getFileExtension(filename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("不支持的文件扩展名: filename={}, extension={}", filename, extension);
            throw new IllegalArgumentException(
                    "不支持的文件类型，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // 2. 检查 MIME type（如果提供）
        if (contentType != null && !contentType.isEmpty()) {
            // 移除可能的参数（如 "text/plain; charset=utf-8" -> "text/plain"）
            String normalizedContentType = contentType.split(";")[0].trim();

            if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
                log.warn("不支持的 MIME 类型: filename={}, contentType={}", filename, contentType);
                throw new IllegalArgumentException(
                        "不支持的文件 MIME 类型: " + normalizedContentType);
            }
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（小写），如果没有扩展名则返回 null
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }

        return filename.substring(lastDotIndex + 1);
    }
}
