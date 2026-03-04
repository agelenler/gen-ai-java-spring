package com.genai.java.spring.aiagent.agent.records;

import java.util.Map;

public record ParsedToolResponse(Map<String,Object> payload, boolean isError) {}