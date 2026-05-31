package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.dto.UploadDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    private final Path carImageDir;

    public UploadService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.carImageDir = Path.of(uploadDir).toAbsolutePath().normalize().resolve("car-images");
    }

    public UploadDtos.UploadResponse uploadCarImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw BusinessException.badRequest("仅支持图片文件");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        extension = extension == null ? "png" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BusinessException.badRequest("图片格式不支持");
        }
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(carImageDir);
            Files.copy(file.getInputStream(), carImageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw BusinessException.badRequest("图片保存失败");
        }
        return new UploadDtos.UploadResponse(
                "/uploads/car-images/" + filename,
                filename,
                file.getSize(),
                contentType
        );
    }
}
