package com.genai.java.spring.aiagent.agent.subagents.webguidance.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aiagent.agent.subagents.helper.SubAgentJsonParser;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceFinding;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WebGuidanceJsonParser extends SubAgentJsonParser<WebGuidanceResult> {

    public WebGuidanceJsonParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Parse domain-specific response into WebGuidanceResult.
     * Subclass implementation of the template method from base class.
     *
     * @param jsonContent Validated JSON string
     * @return Structured WebGuidanceResult with findings and confidence level
     * @throws Exception if parsing fails
     */
    @SuppressWarnings("unchecked")
    @Override
    protected WebGuidanceResult doParseGuidanceResult(String jsonContent) throws Exception {
        // Parse JSON into map
        Map<String, Object> resultMap = objectMapper.readValue(jsonContent, Map.class);

        // Parse findings
        List<WebGuidanceFinding> findings = parseFindings(resultMap.get("findings"));
        List<String> citations = parseCitations(resultMap.get("citations"));
        String summary = String.valueOf(resultMap.getOrDefault("summary", ""));
        String confidence = String.valueOf(resultMap.getOrDefault("confidence", "MEDIUM"));
        List<String> missingEvidence = parseList(resultMap.get("missingEvidence"));
        List<String> rejectedResults = parseList(resultMap.get("rejectedResults"));

        logFindings(findings, confidence);

        return new WebGuidanceResult(findings, citations, summary, confidence, missingEvidence, rejectedResults);
    }

    /**
     * Validate JSON schema structure with domain-specific requirements.
     * Ensures all required fields are present and properly formatted.
     *
     * @param jsonContent JSON string to validate
     * @throws JsonProcessingException if JSON is invalid or schema doesn't match
     */
    @Override
    protected void validateJsonSchema(String jsonContent) throws JsonProcessingException {
        // Validate common schema elements
        super.validateJsonSchema(jsonContent);

        JsonNode root = objectMapper.readTree(jsonContent);

        // Validate each finding has required fields specific to WebGuidance
        JsonNode findingsNode = root.get("findings");
        for (JsonNode finding : findingsNode) {
            validateFindingObject(finding, "source", "url", "recommendation");
        }

        log.debug("[web_guidance] JSON schema validation passed");
    }

    @SuppressWarnings("unchecked")
    private List<WebGuidanceFinding> parseFindings(Object findingsObj) {
        List<WebGuidanceFinding> findings = new ArrayList<>();
        if (findingsObj instanceof List<?> findingsList) {
            for (Object item : findingsList) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    WebGuidanceFinding finding = new WebGuidanceFinding(
                            String.valueOf(map.get("source")),
                            String.valueOf(map.get("url")),
                            String.valueOf(map.get("title")),
                            String.valueOf(map.get("relevantGuidance")),
                            String.valueOf(map.get("appliesTo")),
                            String.valueOf(map.get("recommendation")),
                            String.valueOf(map.get("severityImpact"))
                    );
                    findings.add(finding);
                }
            }
        }
        return findings;
    }

    private static void logFindings(List<WebGuidanceFinding> findings, String confidence) {
        log.info("[web_guidance] Parsed {} findings with confidence: {}", findings.size(), confidence);
        for (WebGuidanceFinding finding : findings) {
            log.info("--Finding details with source-- {}", finding.source());
            log.info("url: {}", finding.url());
            log.info("relevantGuidance: {}", finding.relevantGuidance());
            log.info("title: {}", finding.title());
            log.info("recommendation: {}", finding.recommendation());
        }
    }
}

