package com.genai.java.spring.aiagent.tools.asyncevidences;

import com.genai.java.spring.aiagent.agent.subagents.knowledgeevidence.KnowledgeEvidenceAgent;
import com.genai.java.spring.aiagent.agent.subagents.webguidance.WebGuidanceAgent;
import com.genai.java.spring.aiagent.tools.asyncevidences.records.EvidenceBundle;
import com.genai.java.spring.aiagent.tools.asyncevidences.records.EvidenceCollectionArgs;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceArgs;
import com.genai.java.spring.aiagent.tools.knowledgeevidence.records.KnowledgeEvidenceResult;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceArgs;
import com.genai.java.spring.aiagent.tools.webguidance.records.WebGuidanceResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Tool for parallel async evidence collection.
 * Executes RAG (internal knowledge) and WEB (external guidance) searches concurrently,
 * reducing overall execution time and checkpoint overhead.
 *
 * Acts as a unified interface for evidence collection, coordinating the KnowledgeEvidenceAgent
 * and WebGuidanceAgent for parallel execution.
 */
@Slf4j
@Component
public class AsyncEvidenceTools {

    private final KnowledgeEvidenceAgent knowledgeEvidenceAgent;
    private final WebGuidanceAgent webGuidanceAgent;
    private final ObservationRegistry registry;
    private final Executor executor;

    public AsyncEvidenceTools(
            KnowledgeEvidenceAgent knowledgeEvidenceAgent,
            WebGuidanceAgent webGuidanceAgent,
            ObservationRegistry registry,
            @Qualifier("traceableAsyncExecutor") Executor executor) {
        this.knowledgeEvidenceAgent = knowledgeEvidenceAgent;
        this.webGuidanceAgent = webGuidanceAgent;
        this.registry = registry;
        this.executor = executor;
    }

    @Tool(
        name = "collect_evidence",
        description = """
            Collect internal knowledge evidence and optional authoritative web guidance for a security review finding.
            Executes RAG (internal policies) and WEB (external standards) searches in parallel for efficiency.
            Returns a unified bundle containing both internal and external evidence with structured findings, citations, and confidence levels.
            Suitable for comprehensive evidence gathering after posture assessment is complete.
            """
    )
    public EvidenceBundle collectEvidence(EvidenceCollectionArgs args) {
        Observation toolCallObservation = Observation.start("collect_evidence_tool", registry);
        try (Observation.Scope scope = toolCallObservation.openScope()) {
            if (args == null || args.reviewId() == null || args.reviewId().isBlank()) {
                log.warn("collect_evidence called with null or blank reviewId");
                return createEmptyBundle("No reviewId provided");
            }

            log.info("collect_evidence tool invoked for reviewId: {} with {} knowledge topics and {} web topics",
                    args.reviewId(),
                    args.knowledgeTopics() != null ? args.knowledgeTopics().size() : 0,
                    args.webTopics() != null ? args.webTopics().size() : 0);

            // Launch RAG (knowledge evidence) search asynchronously
            CompletableFuture<KnowledgeEvidenceResult> ragFuture =
                    CompletableFuture.supplyAsync(
                            () -> knowledgeEvidenceAgent.generateGuidance(
                                    new KnowledgeEvidenceArgs(
                                            args.knowledgeTopics(),
                                            args.knowledgeContext(),
                                            args.reviewId(),
                                            args.topK()
                                    )
                            ),
                            executor
                    );

            // Launch WEB (external guidance) search asynchronously, or skip if not requested
            CompletableFuture<WebGuidanceResult> webFuture =
                    args.includeWebGuidance()
                            ? CompletableFuture.supplyAsync(
                                    () -> webGuidanceAgent.generateGuidance(
                                            new WebGuidanceArgs(
                                                    args.webTopics(),
                                                    args.webContext(),
                                                    args.reviewId(),
                                                    args.topK()
                                            )
                                    ),
                                    executor
                            )
                            : CompletableFuture.completedFuture(new WebGuidanceResult(
                                    java.util.List.of(),
                                    java.util.List.of(),
                                    "Web guidance not requested",
                                    "LOW",
                                    java.util.List.of(),
                                    java.util.List.of()
                            ));

            // Wait for both futures to complete
            CompletableFuture.allOf(ragFuture, webFuture).join();

            // Extract results
            KnowledgeEvidenceResult ragResult = ragFuture.join();
            WebGuidanceResult webResult = webFuture.join();

            log.info("collect_evidence completed: RAG findings={}, WEB findings={} for reviewId: {}",
                    ragResult.findings().size(),
                    webResult.findings().size(),
                    args.reviewId());

            return new EvidenceBundle(ragResult, webResult);

        } catch (Exception e) {
            log.error("collect_evidence tool execution failed", e);
            return createErrorBundle(e);
        } finally {
            toolCallObservation.stop();
        }
    }

    private EvidenceBundle createEmptyBundle(String reason) {
        KnowledgeEvidenceResult emptyRag = new KnowledgeEvidenceResult(
                java.util.List.of(),
                java.util.List.of(),
                reason,
                "LOW",
                java.util.List.of(),
                java.util.List.of()
        );
        WebGuidanceResult emptyWeb = new WebGuidanceResult(
                java.util.List.of(),
                java.util.List.of(),
                reason,
                "LOW",
                java.util.List.of(),
                java.util.List.of()
        );
        return new EvidenceBundle(emptyRag, emptyWeb);
    }

    private EvidenceBundle createErrorBundle(Exception e) {
        String errorMsg = "Evidence collection failed: " + e.getMessage();
        KnowledgeEvidenceResult errorRag = new KnowledgeEvidenceResult(
                java.util.List.of(),
                java.util.List.of(),
                errorMsg,
                "LOW",
                java.util.List.of("ERROR: " + e.getClass().getSimpleName()),
                java.util.List.of()
        );
        WebGuidanceResult errorWeb = new WebGuidanceResult(
                java.util.List.of(),
                java.util.List.of(),
                errorMsg,
                "LOW",
                java.util.List.of("ERROR: " + e.getClass().getSimpleName()),
                java.util.List.of()
        );
        return new EvidenceBundle(errorRag, errorWeb);
    }
}

