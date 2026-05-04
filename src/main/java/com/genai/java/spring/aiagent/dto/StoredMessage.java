package com.genai.java.spring.aiagent.dto;

import java.util.Map;

public record StoredMessage(
        String role,            // system | user | assistant | tool
        String content,
        String name,            // optional (tool name, etc.)
        Map<String, Object> metadata
) { }
