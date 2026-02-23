package com.genai.java.spring.rag.config.data;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public class PgVectorStoreConfigData {

    private String tableNameForChatMemory;

    private String tableNameForRag;

    /** Enable / initialize the pgvector schema (default: false) */
    private boolean initializeSchema;

    /** Index type, e.g. HNSW */
    private String indexType;

    /** Distance type, e.g. COSINE_DISTANCE */
    private String distanceType;

    /** Vector dimensionality (optional; can be autodetected) */
    private Integer dimensions;

    /** Maximum number of documents to add in a single batch */
    private Integer maxDocumentBatchSize;
}
