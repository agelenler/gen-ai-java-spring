package com.genai.java.spring.aiagent.tools.posture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class McpContentUtils {

    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Object>> DATA_MAP = new TypeReference<Map<String, Object>>() {
    };

    public McpContentUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts the first JSON payload as a Map. Falls back to text.
     */
    public Map<String, Object> asMap(List<McpSchema.Content> parts) {
        if (parts == null || parts.isEmpty()) {
            return Map.of("error", "EMPTY_CONTENT");
        }

        for (McpSchema.Content content : parts) {
            if (content instanceof McpSchema.TextContent textContent) {
                String text = textContent.text();
                if (text != null) {
                    String trimmed = text.trim();
                    try {
                        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                            return objectMapper.readValue(trimmed, DATA_MAP);
                        }
                    } catch (Exception ignore) {
                        // not JSON — fall through
                    }
                    return Map.of("text", text);
                }
            }
        }

        McpSchema.Content first = parts.getFirst();
        return Map.of("type", first.getClass().getSimpleName());
    }
}
