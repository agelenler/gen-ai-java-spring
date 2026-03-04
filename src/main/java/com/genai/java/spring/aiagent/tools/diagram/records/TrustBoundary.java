package com.genai.java.spring.aiagent.tools.diagram.records;

import java.util.List;

public record TrustBoundary(
        String name,
        List<String> includes // node IDs within the boundary
) {}