package com.genai.java.spring.rag.rerank.exception;

public class RerankException extends RuntimeException {

    public RerankException(String message, Throwable cause) {
        super(message, cause);
    }

    public RerankException(String message) {
        super(message);
    }
}
