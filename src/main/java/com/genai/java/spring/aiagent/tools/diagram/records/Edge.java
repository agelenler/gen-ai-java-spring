package com.genai.java.spring.aiagent.tools.diagram.records;

public record Edge(
    String from,
    String to,
    String protocol,     // e.g., "HTTPS", "JDBC", "gRPC"
    Integer port,
    String auth,         // e.g., "OAuth2", "mTLS", "None"
    Boolean encrypted,   // true if encrypted in transit
    String notes
) {}