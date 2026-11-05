package com.genai.java.spring.aiagent.agent.subagents.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Base class for sub-agent JSON parsers with common reusable parsing logic.
 * Provides shared methods for extracting JSON, parsing lists, validating common schema elements,
 * and a template method for parsing responses into typed results.
 *
 * @param <R> The result type (e.g., KnowledgeEvidenceResult, WebGuidanceResult)
 */
@Slf4j
public abstract class SubAgentJsonParser<R> {

    protected final ObjectMapper objectMapper;

    protected SubAgentJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Template method for parsing LLM response into structured result.
     * Handles common operations: extract JSON, validate schema, and delegate to subclass parsing.
     * Subclasses should override parseGuidanceResultImpl() to provide domain-specific parsing logic.
     *
     * @param response LLM response (may contain markdown/explanation with JSON)
     * @return Structured result of type R with findings and confidence level
     * @throws Exception if JSON is invalid or schema validation fails
     */
    public R parseGuidanceResult(String response) throws Exception {
        // Extract JSON from response (may contain markdown/explanation)
        String jsonContent = extractJsonFromResponse(response);

        // Validate JSON structure before parsing
        validateJsonSchema(jsonContent);

        // Delegate to subclass-specific parsing logic
        return doParseGuidanceResult(jsonContent);
    }

    /**
     * Validate JSON schema structure before parsing.
     * Calls validateCommonSchema() for common fields, then delegates to subclass for domain-specific validation.
     *
     * @param jsonContent JSON string to validate
     * @throws JsonProcessingException if JSON is invalid or schema doesn't match
     */
    protected void validateJsonSchema(String jsonContent) throws JsonProcessingException {
        // Validate common schema elements
        validateCommonSchema(jsonContent);
        // Subclasses should override to add domain-specific validation
    }

    /**
     * Parse citations list from response.
     * Converts each item to String and collects into a list.
     *
     * @param citationsObj Object to parse (typically a List)
     * @return List of citation strings, or empty list if input is null/invalid
     */
    protected List<String> parseCitations(Object citationsObj) {
        if (citationsObj instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * Parse a generic string list from response.
     * Converts each item to String and collects into a list.
     *
     * @param listObj Object to parse (typically a List)
     * @return List of strings, or empty list if input is null/invalid
     */
    protected List<String> parseList(Object listObj) {
        if (listObj instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /**
     * Validate common required fields and structure in JSON response.
     * This includes checking for root object, required top-level fields, and confidence value.
     * Subclasses should override to add domain-specific field validation.
     *
     * @param jsonContent JSON string to validate
     * @throws JsonProcessingException if validation fails
     */
    protected void validateCommonSchema(String jsonContent) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonContent);

        // Validate root is an object
        if (!root.isObject()) {
            throw new IllegalArgumentException("Root must be a JSON object");
        }

        // Validate required fields exist
        if (!root.has("findings")) {
            throw new IllegalArgumentException("Missing required field: findings");
        }

        if (!root.has("summary")) {
            throw new IllegalArgumentException("Missing required field: summary");
        }

        if (!root.has("confidence")) {
            throw new IllegalArgumentException("Missing required field: confidence");
        }

        // Validate findings is an array
        JsonNode findingsNode = root.get("findings");
        if (!findingsNode.isArray()) {
            throw new IllegalArgumentException("Field 'findings' must be an array");
        }

        // Validate citations is an array if present
        if (root.has("citations") && !root.get("citations").isArray()) {
            throw new IllegalArgumentException("Field 'citations' must be an array");
        }

        // Validate confidence value
        String confidence = root.get("confidence").asText();
        if (!List.of("HIGH", "MEDIUM", "LOW").contains(confidence)) {
            throw new IllegalArgumentException("Confidence must be one of: HIGH, MEDIUM, LOW");
        }

        log.debug("JSON schema validation passed - common fields validated");
    }

    /**
     * Validate each finding has the required fields.
     * Subclasses should call this with the finding node and their specific required fields.
     *
     * @param finding JsonNode representing a finding object
     * @param requiredFields Variable number of required field names
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateFindingObject(JsonNode finding, String... requiredFields) {
        if (!finding.isObject()) {
            throw new IllegalArgumentException("Each finding must be a JSON object");
        }

        for (String field : requiredFields) {
            if (!finding.has(field)) {
                throw new IllegalArgumentException("Finding missing required field: " + field);
            }
        }
    }

    /**
     * Subclass-specific parsing logic.
     * Called after common extraction and validation steps.
     * Subclasses should parse the validated JSON content and return their specific result type.
     *
     * @param jsonContent Validated JSON string
     * @return Structured result of type R
     * @throws Exception if parsing fails
     */
    protected abstract R doParseGuidanceResult(String jsonContent) throws Exception;

    /**
     * Extract JSON from response (handle cases where LLM includes markdown/explanation).
     * Looks for JSON block delimited by { and }.
     *
     * @param response LLM response which may contain markdown or explanatory text
     * @return Extracted JSON string
     */
    private String extractJsonFromResponse(String response) {
        // Look for JSON block: {...}
        int startIdx = response.indexOf('{');
        int endIdx = response.lastIndexOf('}');
        if (startIdx >= 0 && endIdx > startIdx) {
            return response.substring(startIdx, endIdx + 1);
        }
        // If no JSON found, assume entire response is JSON
        return response;
    }

}

