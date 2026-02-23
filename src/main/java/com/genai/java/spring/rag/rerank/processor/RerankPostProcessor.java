package com.genai.java.spring.rag.rerank.processor;

import com.genai.java.spring.rag.config.data.RagConfigData;
import com.genai.java.spring.rag.rerank.client.RerankerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DocumentPostProcessor} that reorders retrieved documents using a remote
 * reranker service.
 *
 * <p>The processor queries the {@link RerankerClient} for scores for each document's
 * formatted content and sorts documents by descending score. If the reranker fails
 * the processor is fail-open and returns the original list of documents.</p>
 *
 * <p>Top-N selection is controlled by {@link RagConfigData#getRerank()}</p>
 */

@Slf4j
@Component
public class RerankPostProcessor implements DocumentPostProcessor {

    private final RerankerClient rerankerClient;
    private final RagConfigData ragConfigData;

    public RerankPostProcessor(RerankerClient rerankerClient, RagConfigData ragConfigData) {
        this.rerankerClient = rerankerClient;
        this.ragConfigData = ragConfigData;
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (documents.isEmpty()) {
            return documents;
        }
        try {
            double[] scores = getScores(query, documents);
            List<Integer> indices = sortIndices(documents, scores);
            return getTopNDocuments(documents, indices);
        } catch (Exception e) {
            log.warn("Reranker error, returning original documents!", e);
            //if reranker fails, don't block the answer
            return documents;
        }
    }

    private double[] getScores(Query query, List<Document> documents) {
        List<String> texts = documents.stream().map(Document::getFormattedContent).toList();
        return rerankerClient.score(query.text(), texts);
    }

    private List<Integer> sortIndices(List<Document> documents, double[] scores) {
        List<Integer> indices = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            indices.add(i);
        }
        indices.sort((i, j) -> Double.compare(scores[j], scores[i]));
        return indices;
    }

    private List<Document> getTopNDocuments(List<Document> documents, List<Integer> indices) {
        int topN = Math.min(ragConfigData.getRerank().getTopN(), documents.size());
        List<Document> topNDocuments = new ArrayList<>(topN);
        for (int i = 0; i < topN; i++) {
            topNDocuments.add(documents.get(indices.get(i)));
        }
        return topNDocuments;
    }
}
