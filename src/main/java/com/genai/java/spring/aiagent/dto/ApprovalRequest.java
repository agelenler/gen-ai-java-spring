package com.genai.java.spring.aiagent.dto;

import java.util.Map;

public record ApprovalRequest(
        boolean approved,
        String note,
        Map<String, Object> edits
) { }
