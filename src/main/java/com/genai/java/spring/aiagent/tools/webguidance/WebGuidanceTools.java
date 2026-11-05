package com.genai.java.spring.aiagent.tools.webguidance;

import com.genai.java.spring.aiagent.agent.subagents.webguidance.WebGuidanceAgent;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceArgs;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebGuidanceTools {

    private final WebGuidanceAgent webGuidanceAgent;

    public WebGuidanceTools(WebGuidanceAgent webGuidanceAgent) {
        this.webGuidanceAgent = webGuidanceAgent;
    }

    @Tool(
            name = "web_guidance",
            description = """
                    Use for authoritative OWASP/NIST/CWE/RFC guidance to improve confidence in a security review finding.
                    Accepts multiple topics for decomposed search strategy (e.g., ["OWASP secrets", "NIST secrets", "CWE hardcoded credentials"]).
                    Returns distilled findings, citations, and recommendations with confidence levels.
                    Suitable for validating architectural decisions, compliance mappings, or threat model assumptions.
                    """
    )
    public WebGuidanceResult webGuidance(WebGuidanceArgs args) {
        try {
            if (args == null || args.topics() == null || args.topics().isEmpty()) {
                log.warn("web_guidance called with null or empty topics");
                return createEmptyResult("No topics provided");
            }
            log.info("web_guidance tool invoked with {} topics for reviewId: {}", args.topics().size(), args.reviewId());
            return webGuidanceAgent.generateGuidance(args);
        } catch (Exception e) {
            log.error("web_guidance tool execution failed", e);
            return createErrorResult(e);
        }
    }

    private WebGuidanceResult createEmptyResult(String reason) {
        return new WebGuidanceResult(
                java.util.List.of(),
                java.util.List.of(),
                reason,
                "LOW",
                java.util.List.of(),
                java.util.List.of()
        );
    }

    private WebGuidanceResult createErrorResult(Exception e) {
        return new WebGuidanceResult(
                java.util.List.of(),
                java.util.List.of(),
                "Web guidance retrieval failed: " + e.getMessage(),
                "LOW",
                java.util.List.of("ERROR: " + e.getClass().getSimpleName()),
                java.util.List.of()
        );
    }
}
