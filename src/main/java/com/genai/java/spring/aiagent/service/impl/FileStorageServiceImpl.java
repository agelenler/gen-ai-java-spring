package com.genai.java.spring.aiagent.service.impl;

import com.genai.java.spring.aiagent.config.data.AIAgentConfigData;
import com.genai.java.spring.aiagent.exception.SecurityReviewAgentException;
import com.genai.java.spring.aiagent.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final AIAgentConfigData aiAgentConfigData;
    private final Path root;

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg", "pdf", "drawio", "puml");

    public FileStorageServiceImpl(AIAgentConfigData aiAgentConfigData) throws IOException {
        this.aiAgentConfigData = aiAgentConfigData;
        root = Path.of(aiAgentConfigData.getUploadDir());
        Files.createDirectories(root);
    }

    @Override
    public String save(MultipartFile file) throws SecurityReviewAgentException {
        if (file.isEmpty()) {
            throw new SecurityReviewAgentException("Empty upload!");
        }
        String ext = inferExtension(file);
        if (ext == null || !ALLOWED_EXT.contains(ext)) {
            throw new SecurityReviewAgentException("Unsupported file type: " + file.getContentType() + " (ext=" + ext + ")");
        }

        String id = UUID.randomUUID().toString();
        String fileName = id + "." + ext;
        Path targetPath = root.resolve(fileName).normalize();

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new SecurityReviewAgentException("Could not save the file: " + file.getName(), e);
        }
        return file.getName();
    }

    @Override
    public Path resolve(String fileName) {
        return root.resolve(fileName);
    }

    private String inferExtension(MultipartFile file) {
        // 1) from original filename
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            if (!ext.isBlank()) return ext;
        }
        // 2) from content-type
        String contentType = file.getContentType();
        if (contentType != null) {
            return switch (contentType) {
                case "image/png"          -> "png";
                case "image/jpeg"         -> "jpg";
                case "image/webp"         -> "webp";
                case "image/gif"          -> "gif";
                case "image/svg+xml"      -> "svg";
                case "application/pdf"    -> "pdf";
                case "application/xml", "text/xml" -> "drawio";   // if you only accept Draw.io XMLs
                case "text/plain"         -> "puml";              // if PlantUML uploads are text
                default -> null;
            };
        }
        // 3) last resort: unknown
        return null;
    }
}
