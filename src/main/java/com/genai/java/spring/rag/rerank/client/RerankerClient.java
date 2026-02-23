package com.genai.java.spring.rag.rerank.client;

import com.genai.java.spring.rag.rerank.exception.RerankException;

import java.util.List;

public interface RerankerClient {

    double[] score(String query, List<String> documents) throws RerankException;
}
