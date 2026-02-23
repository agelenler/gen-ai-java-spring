package com.genai.java.spring.rag.config.postprocessor;

import com.genai.java.spring.rag.config.data.RagConstants;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CitationHeaderPostProcessor implements DocumentPostProcessor {

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        List<Document> updatedDocuments = new ArrayList<>(documents.size());
        for (var document : documents) {
            Map<String, Object> metadata = new HashMap<>(document.getMetadata());
            String newContent = "[" + RagConstants.SOURCE + ":" + getSource(metadata) + ", " + RagConstants.PAGE_SNAKE +
                    ":" + getPageNumber(metadata) + "]\n" + document.getFormattedContent();
            updatedDocuments.add(new Document(newContent, metadata));
        }
        return updatedDocuments;
    }

    private String getSource(Map<String, Object> metadata) {
        return String.valueOf(metadata.getOrDefault(RagConstants.SOURCE, RagConstants.UNKNOWN));
    }

    private String getPageNumber(Map<String, Object> metadata) {
        Object pageNumber = metadata.get(RagConstants.PAGE_SNAKE);
        if (pageNumber == null) {
            pageNumber = metadata.get(RagConstants.PAGE_CAMEL);
        }
        if (pageNumber == null) {
            pageNumber = metadata.get(RagConstants.PAGE);
        }
        return pageNumber == null ? RagConstants.UNKNOWN : String.valueOf(pageNumber);
    }


}
