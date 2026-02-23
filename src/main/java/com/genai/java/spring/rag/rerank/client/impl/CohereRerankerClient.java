package com.genai.java.spring.rag.rerank.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.genai.java.spring.rag.config.data.RagConfigData;
import com.genai.java.spring.rag.rerank.client.RerankerClient;
import com.genai.java.spring.rag.rerank.exception.RerankException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class CohereRerankerClient implements RerankerClient {
    private final RagConfigData.RerankProperties rerankProperties;
    private final ObjectMapper objectMapper;

    public CohereRerankerClient(RagConfigData ragConfigData, ObjectMapper objectMapper) {
        this.rerankProperties = ragConfigData.getRerank();
        this.objectMapper = objectMapper;
    }

    @Override
    public double[] score(String query, List<String> documents) throws RerankException {
        ObjectNode root = getJsonEntityFromRerank(query, documents);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            JsonNode node = executeRerankRequest(root, httpClient);
            double[] scores = getScores(documents, node);
            setScoresForUntouched(scores);
            return scores;
        } catch (IOException e) {
            throw new RerankException("Exception during rerank operation!", e);
        }


    }

    private ObjectNode getJsonEntityFromRerank(String query, List<String> documents) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", this.rerankProperties.getModel());
        root.put("query", query);
        ArrayNode docs = objectMapper.createArrayNode();
        for (String d : documents) {
            docs.add(d);
        }
        root.set("documents", docs);
        root.put("return_documents", false); // only return the scores
        return root;
    }

    private JsonNode executeRerankRequest(ObjectNode root, CloseableHttpClient httpClient) throws IOException {
        HttpPost httpPostRequest = new HttpPost(this.rerankProperties.getUrl());
        httpPostRequest.addHeader("Authorization", "Bearer " + this.rerankProperties.getApiKey());
        httpPostRequest.addHeader("Content-Type", "application/json");
        httpPostRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8));
        return httpClient.execute(httpPostRequest, response -> {
            try (HttpEntity entity = response.getEntity()) {
                return objectMapper.readTree(entity.getContent());
            }
        });
    }

    private double[] getScores(List<String> documents, JsonNode node) {
        var rerankResults = node.path("results");
        double[] scores = new double[documents.size()];
        Arrays.fill(scores, Double.NEGATIVE_INFINITY); // initialize to very low
        for (var rerankResult : rerankResults) {
            int index = rerankResult.path("index").asInt();
            double score = rerankResult.path("relevance_score").asDouble();
            if (index >= 0 && index < scores.length) {
                scores[index] = score;
            }
        }
        return scores;
    }

    private void setScoresForUntouched(double[] scores) {
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] == Double.NEGATIVE_INFINITY) {
                scores[i] = -1.0; //set to -1 so they sort last
            }
        }
    }
}
