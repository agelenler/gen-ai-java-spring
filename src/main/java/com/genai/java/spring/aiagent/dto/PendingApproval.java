package com.genai.java.spring.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingApproval {
    private ApprovalType type;
    private String message;                 // what to ask the human
    private Map<String, Object> payload;    // extracted services/edges
}
