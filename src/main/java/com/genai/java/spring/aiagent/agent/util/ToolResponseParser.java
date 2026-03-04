package com.genai.java.spring.aiagent.agent.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aiagent.agent.records.ParsedToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public final class ToolResponseParser {

  private final ObjectMapper objectMapper;

  private static final TypeReference<Map<String,Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

  private ToolResponseParser(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
  }

  /** Returns a payload Map and an isError flag (from MCP if present). */
  public ParsedToolResponse parse(String responseData) {
    if (responseData == null || responseData.isBlank()) {
      return new ParsedToolResponse(Map.of("error","EMPTY_CONTENT"), false);
    }

    // 1) If it's already a JSON object → Map
    try {
      JsonNode root = objectMapper.readTree(responseData);

      // Case A: CallToolResult wrapper: { "content":[...], "isError": true/false }
      if (root.isObject() && root.has("content") && root.get("content").isArray()) {
        boolean isError = root.has("isError") && root.get("isError").asBoolean(false);
        Map<String,Object> payload = fromContentParts(root.get("content"));
        return new ParsedToolResponse(payload, isError);
      }

      // Case B: array of content parts → unwrap first json/text
      if (root.isArray()) {
        Map<String,Object> payload = fromContentParts(root);
        return new ParsedToolResponse(payload, false);
      }

      // Case C: plain object → Map
      if (root.isObject()) {
        return new ParsedToolResponse(objectMapper.convertValue(root, MAP_TYPE_REFERENCE), false);
      }

      // Fallback: treat as text
      return new ParsedToolResponse(Map.of("text", responseData), false);
    } catch (Exception e) {
      // Not valid JSON; return as text
      return new ParsedToolResponse(Map.of("text", responseData), false);
    }
  }

  /** Extract Map from content parts: prefer {"json":{...}}, else {"text":"{...}"} parsed as JSON. */
  private Map<String,Object> fromContentParts(JsonNode parts) {
    if (parts.isArray() && parts.size() > 0) {
      JsonNode first = parts.get(0);

      // Prefer JSON content
      if (first.hasNonNull("json")) {
        return objectMapper.convertValue(first.get("json"), MAP_TYPE_REFERENCE);
      }

      // Fallback: text that might be JSON
      if (first.hasNonNull("text")) {
        String txt = first.get("text").asText("");
        String t = txt.trim();
        if (!t.isEmpty() && (t.startsWith("{") || t.startsWith("["))) {
          try { return objectMapper.readValue(t, MAP_TYPE_REFERENCE); } catch (Exception ignore) { /* fall through */ }
        }
        return Map.of("text", txt);
      }
    }
    return Map.of("error","EMPTY_CONTENT_PARTS");
  }

}
