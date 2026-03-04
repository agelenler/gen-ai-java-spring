package com.genai.java.spring.aiagent.tools.diagram.records;

public record DataStore(
    String id,
    String type, // e.g., "postgres", "s3", "redis"
    String classification,  // e.g., "PII", "Logs", "Payments"
    Boolean encryptedAtRest
) {}