package com.genai.java.spring.aiagent.service;

import com.genai.java.spring.aiagent.exception.SecurityReviewAgentException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    String save(MultipartFile file) throws SecurityReviewAgentException;

    Path resolve(String fileName);
}
