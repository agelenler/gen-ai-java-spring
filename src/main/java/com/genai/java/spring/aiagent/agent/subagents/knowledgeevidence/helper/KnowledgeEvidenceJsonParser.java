package com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.aiagent.agent.subagents.helper.SubAgentJsonParser;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceFinding;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
/**
 * The KnowledgeEvidenceJsonParser class is responsible for parsing JSON responses
 * specific to the Knowledge Evidence domain. It extends the SubAgentJsonParser
 * to provide domain-specific parsing logic for transforming JSON content into
 * structured KnowledgeEvidenceResult objects.
 *
 * <p>Usage:</p>
 * This class is used to parse JSON responses containing findings, citations,
 * summaries, confidence levels, and other related data. It ensures that the
 * JSON schema is validated and the data is transformed into domain-specific
 * objects for further processing.
 *
 * <p>Key Methods:</p>
 * <ul>
 *   <li>{@link #doParseGuidanceResult(String)}: Parses the JSON content into a
 *       KnowledgeEvidenceResult object, extracting findings, citations, and other
 *       relevant fields.</li>
 *   <li>{@link #validateJsonSchema(String)}: Validates the JSON schema to ensure
 *       it meets domain-specific requirements.</li>
 *   <li>{@link #parseFindings(Object)}: Extracts and transforms findings from the
 *       JSON content into KnowledgeEvidenceFinding objects.</li>
 *   <li>{@link #logFindings(List, String)}: Logs the parsed findings and confidence
 *       level for debugging and traceability.</li>
 * </ul>
 */
public class KnowledgeEvidenceJsonParser extends SubAgentJsonParser<KnowledgeEvidenceResult> {

    public KnowledgeEvidenceJsonParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Parse domain-specific response into KnowledgeEvidenceResult.
     * Subclass implementation of the template method from base class.
     *
     * @param jsonContent Validated JSON string
     * @return Structured KnowledgeEvidenceResult with findings and confidence level
     * @throws Exception if parsing fails
     */
    @SuppressWarnings("unchecked")
    @Override
    protected KnowledgeEvidenceResult doParseGuidanceResult(String jsonContent) throws Exception {
        // Parse JSON into map
        Map<String, Object> resultMap = objectMapper.readValue(jsonContent, Map.class);

        // Parse findings
        List<KnowledgeEvidenceFinding> findings = parseFindings(resultMap.get("findings"));
        List<String> citations = parseCitations(resultMap.get("citations"));
        String summary = String.valueOf(resultMap.getOrDefault("summary", ""));
        String confidence = String.valueOf(resultMap.getOrDefault("confidence", "MEDIUM"));
        List<String> missingEvidence = parseList(resultMap.get("missingEvidence"));
        List<String> rejectedResults = parseList(resultMap.get("rejectedResults"));

        logFindings(findings, confidence);

        return new KnowledgeEvidenceResult(findings, citations, summary, confidence, missingEvidence, rejectedResults);
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

        // Validate each finding has required fields specific to KnowledgeEvidence
        JsonNode findingsNode = root.get("findings");
        for (JsonNode finding : findingsNode) {
            validateFindingObject(finding, "docId", "source", "recommendation");
        }

        log.debug("[knowledge_evidence] JSON schema validation passed");
    }

    @SuppressWarnings("unchecked")
    private List<KnowledgeEvidenceFinding> parseFindings(Object findingsObj) {
        List<KnowledgeEvidenceFinding> findings = new ArrayList<>();
        if (findingsObj instanceof List<?> findingsList) {
            for (Object item : findingsList) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    KnowledgeEvidenceFinding finding = new KnowledgeEvidenceFinding(
                            String.valueOf(map.get("docId")),
                            String.valueOf(map.get("source")),
                            String.valueOf(map.get("title")),
                            String.valueOf(map.get("relevantGuidance")),
                            String.valueOf(map.get("appliesTo")),
                            String.valueOf(map.get("recommendation")),
                            String.valueOf(map.get("severityImpact")),
                            Double.parseDouble(String.valueOf(map.getOrDefault("score", 0.0)))
                    );
                    findings.add(finding);
                }
            }
        }
        return findings;
    }

    private void logFindings(List<KnowledgeEvidenceFinding> findings, String confidence) {
        log.info("[knowledge_evidence] Parsed {} findings with confidence: {}", findings.size(), confidence);
        for (KnowledgeEvidenceFinding finding : findings) {
            log.info("--Finding details from source-- {}", finding.source());
            log.info("docId: {}", finding.docId());
            log.info("relevantGuidance: {}", finding.relevantGuidance());
            log.info("title: {}", finding.title());
            log.info("recommendation: {}", finding.recommendation());
        }
    }
}
