package com.genai.java.spring.rag.config.postprocessor;

import com.genai.java.spring.rag.config.data.RagConfigData;
import com.genai.java.spring.rag.config.data.RagConstants;
import com.genai.java.spring.rag.config.postprocessor.record.Target;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds neighbor chunks (chunk_index ± N) for each retrieved doc, by source.
 * Assumes metadata contains: source (String), chunk_index (Number/int).
 */
@Component
public class NeighbourStitchPostProcessor implements DocumentPostProcessor {
    private final VectorStore vectorStore;
    private final RagConfigData ragConfigData;

    public NeighbourStitchPostProcessor(@Qualifier("ragVectorStore") VectorStore vectorStore,
                                        RagConfigData ragConfigData) {
        this.vectorStore = vectorStore;
        this.ragConfigData = ragConfigData;
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (documents.isEmpty()) {
            return documents;
        }
        Set<Target> targets = getTargetDocuments(documents);
        List<Document> neighbours = getNeighbourDocuments(targets);
        List<Document> merged = getMergedDocuments(documents, neighbours);
        return dedupBySourceIndexPreserveOrder(merged);
    }

    private Set<Target> getTargetDocuments(List<Document> documents) {
        Set<Target> targets = new LinkedHashSet<>();

        for (var document : documents) {
            String source = String.valueOf(document.getMetadata().get(RagConstants.SOURCE));
            Object indexObj = document.getMetadata().get(RagConstants.CHUNK_INDEX);
            if (source == null || indexObj == null) {
                continue;
            }
            int index = (indexObj instanceof Number) ? ((Number) indexObj).intValue()
                    : Integer.parseInt(String.valueOf(indexObj));
            for (int delta = 1; delta <= ragConfigData.getRadius(); delta++) {
                targets.add(new Target(source, index - delta));
                targets.add(new Target(source, index + delta));
            }
        }

        return targets;
    }

    private List<Document> getNeighbourDocuments(Set<Target> targets) {
        List<Document> neighbours = new ArrayList<>();
        for (var targetDocument : targets) {
            var searchRequest = getSearchRequest(targetDocument);
            var hits = vectorStore.similaritySearch(searchRequest);
            if (!hits.isEmpty()) {
                neighbours.addAll(hits);
            }
        }
        return neighbours;
    }

    private SearchRequest getSearchRequest(Target targetDocument) {
        String filter = "%s == '%s' && %s == %d"
                .formatted(RagConstants.SOURCE, targetDocument.source().replace("'", "\\'"),
                        RagConstants.CHUNK_INDEX, targetDocument.index());
        return SearchRequest.builder()
                .query("__neighbour__")
                .topK(1)
                .similarityThreshold(0.0)
                .filterExpression(filter)
                .build();
    }

    private List<Document> getMergedDocuments(List<Document> documents, List<Document> neighbours) {
        List<Document> merged = new ArrayList<>(documents.size() + neighbours.size());
        merged.addAll(documents);
        merged.addAll(neighbours);
        return merged;
    }

    private List<Document> dedupBySourceIndexPreserveOrder(List<Document> merged) {
        Set<String> seen = new HashSet<>();
        List<Document> outputs = new ArrayList<>(merged.size());
        for (var document : merged) {
            String source = String.valueOf(document.getMetadata().get(RagConstants.SOURCE));
            Object indexObj = document.getMetadata().get(RagConstants.CHUNK_INDEX);
            String key = source + "#" + indexObj;
            if (seen.add(key)) {
                outputs.add(document);
            }
        }
        return outputs;
    }
}
