package com.genai.java.spring.aiagent.tools.knowledgeevidence;

import com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.KnowledgeEvidenceAgent;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceArgs;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KnowledgeEvidenceTools {

    private final KnowledgeEvidenceAgent knowledgeEvidenceAgent;

    public KnowledgeEvidenceTools(KnowledgeEvidenceAgent knowledgeEvidenceAgent) {
        this.knowledgeEvidenceAgent = knowledgeEvidenceAgent;
    }

    @Tool(
            name = "knowledge_evidence",
            description = """
                    Use for internal policy and handbook guidance to improve confidence in a security review finding.
                    Accepts multiple topics for decomposed search strategy (e.g., ["password policy", "encryption standards", "API security"]).
                    Returns distilled findings, citations, and recommendations with confidence levels from internal knowledge base.
                    Suitable for validating architectural decisions against internal policies, compliance mappings, or internal standards.
                    """

    )
    public KnowledgeEvidenceResult knowledgeEvidence(KnowledgeEvidenceArgs args) {
        try {
            if (args == null || args.topics() == null || args.topics().isEmpty()) {
                log.warn("knowledge_evidence called with null or empty topics");
                return createEmptyResult("No topics provided");
            }
            log.info("knowledge_evidence tool invoked with {} topics for reviewId: {}", args.topics().size(), args.reviewId());
            return knowledgeEvidenceAgent.generateGuidance(args);
        } catch (Exception e) {
            log.error("knowledge_evidence tool execution failed", e);
            return createErrorResult(e);
        }
    }

    private KnowledgeEvidenceResult createErrorResult(Exception exception) {
        return new KnowledgeEvidenceResult(
                java.util.List.of(),
                java.util.List.of(),
                "RAG guidance retrieval failed: " + exception.getMessage(),
                "LOW",
                java.util.List.of("ERROR: " + exception.getClass().getSimpleName()),
                java.util.List.of()
        );
    }

    private KnowledgeEvidenceResult createEmptyResult(String reason) {
        return new KnowledgeEvidenceResult(
                java.util.List.of(),
                java.util.List.of(),
                reason,
                "LOW",
                java.util.List.of(),
                java.util.List.of()
        );
    }
}
