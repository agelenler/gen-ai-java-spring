package com.genai.java.spring.aiagent.tools.rag;

import com.genai.java.spring.aiagent.config.data.AIAgentConfigData;
import com.genai.java.spring.aiagent.tools.rag.records.RagArgs;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class RagTools {

    private final VectorStore vectorStore;
    private final AIAgentConfigData.RagToolProperties ragToolProperties;
    private final ObservationRegistry registry;

    public RagTools(@Qualifier("ragVectorStore") VectorStore vectorStore,
                    AIAgentConfigData aiAgentConfigData,
                    ObservationRegistry registry) {
        this.vectorStore = vectorStore;
        this.ragToolProperties = aiAgentConfigData.getRagTool();
        this.registry = registry;
    }

    @Tool(name = "rag_query", description = "Query internal security policies and checklists with RAG; return concise quotes + citations.")
    Map<String, Object> query(RagArgs ragArgs) {
        try {
            Observation toolCallObservation = Observation.start("rag_query", registry);
            try (Observation.Scope scope = toolCallObservation.openScope()) {
                if (ragArgs == null) {
                    return Collections.emptyMap();
                }
                log.info("Calling rag_query tool with question: {} and topK: {}", ragArgs.question(), ragArgs.topK());
                SearchRequest.Builder searchRequestBuilder = getSearchRequestBuilder(ragArgs, getTopK(ragArgs));
                var hits = vectorStore.similaritySearch(searchRequestBuilder.build());
                return Map.of("matches", getMatches(hits));
            } finally {
                toolCallObservation.stop();
            }
        } catch (Exception e) {
            return Map.of("error", "RAG_SEARCH_FAILED", "message", e.getMessage());
        }
    }

    private int getTopK(RagArgs ragArgs) {
        return Math.max(this.ragToolProperties.getMinTopK(), Math.min(this.ragToolProperties.getMaxTopK(),
                Optional.ofNullable(ragArgs.topK()).orElse(this.ragToolProperties.getDefaultTopK())));
    }

    private SearchRequest.Builder getSearchRequestBuilder(RagArgs ragArgs, int topK) {
        return SearchRequest.builder()
                .query(Objects.requireNonNull(ragArgs.question(), "question is required"))
                .topK(topK)
                .similarityThreshold(this.ragToolProperties.getSimilarityThreshold());
    }

    private List<Map<String, Object>> getMatches(List<Document> hits) {
        return hits.stream().map(document -> Map.of(
                "docId", document.getId(),
                "title", document.getMetadata().getOrDefault("title", ""),
                "score", document.getScore(),
                "quote", document.getFormattedContent(),
                "source", Map.of(
                        "uri", document.getMetadata().getOrDefault("uri", ""),
                        "section", document.getMetadata().getOrDefault("section", "")
                )
        )).toList();
    }
}
