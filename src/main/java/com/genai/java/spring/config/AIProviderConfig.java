package com.genai.java.spring.config;

import com.genai.java.spring.aiagent.advisor.ContentSanitizerAdvisor;
import com.genai.java.spring.chat.advisor.ErrorWrappingAdvisor;
import com.genai.java.spring.chat.advisor.SystemPromptAdvisor;
import com.genai.java.spring.chat.advisor.ValidationAdvisor;
import com.genai.java.spring.rag.config.data.PgVectorStoreConfigData;
import com.genai.java.spring.rag.config.data.RagConfigData;
import com.genai.java.spring.rag.config.postprocessor.CitationHeaderPostProcessor;
import com.genai.java.spring.rag.config.postprocessor.NeighbourStitchPostProcessor;
import com.genai.java.spring.rag.config.preprocessor.DomainSynonymTransformer;
import com.genai.java.spring.rag.rerank.processor.RerankPostProcessor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
public class AIProviderConfig {

    @Value("classpath:/templates/vector-store-memory-system-prompt.st")
    private Resource vectorStoreMemorySystemPrompt;
    private static final int TOP_K = 10;

    @Value("classpath:/templates/prompt-chat-memory-system-prompt.st")
    private Resource promptChatMemorySystemPrompt;

    @Value("classpath:/templates/query-expander-prompt.st")
    private Resource queryExpanderPrompt;

    @Value("${spring.ai.openai.chat.options.model-vision}")
    private String openAIVisionModel;

    private static final int MAX_MESSAGES = 5;

    @Bean("chatMemoryVectorStore")
    public VectorStore chatMemoryVectorStore(JdbcTemplate jdbcTemplate,
                                             PgVectorStoreConfigData pgVectorStoreConfigData,
                                             @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForChatMemory())
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }

    @Bean("ragVectorStore")
    public VectorStore ragVectorStore(JdbcTemplate jdbcTemplate,
                                      PgVectorStoreConfigData pgVectorStoreConfigData,
                                      @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForRag())
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }

    @Bean("queryExpanderChatClientBuilder")
    ChatClient.Builder queryExpanderChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.0) // deterministic rewrite
                        .maxTokens(256) // short paraphrases
                        .build());
    }

    @Bean
    RetrievalAugmentationAdvisor ragAdvisor(@Qualifier("ragVectorStore") VectorStore vectorStore,
                                            RagConfigData ragConfigData,
                                            DomainSynonymTransformer domainSynonymTransformer,
                                            NeighbourStitchPostProcessor neighbourStitchPostProcessor,
                                            CitationHeaderPostProcessor citationHeaderPostProcessor,
                                            RerankPostProcessor rerankPostProcessor,
                                            @Qualifier("queryExpanderChatClientBuilder") ChatClient.Builder queryExpanderChatClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(domainSynonymTransformer)
                .queryExpander(MultiQueryExpander.builder()
                        .chatClientBuilder(queryExpanderChatClientBuilder)
                        .numberOfQueries(ragConfigData.getQueryExpander().getNumberOfQueries())
                        .promptTemplate(new PromptTemplate(queryExpanderPrompt))
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(ragConfigData.getTopK())
                        .similarityThreshold(ragConfigData.getSimilarityThreshold())
                        .build())
                .documentPostProcessors(rerankPostProcessor, neighbourStitchPostProcessor, citationHeaderPostProcessor)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
                        .build())
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter(RagConfigData ragConfigData) {
        return TokenTextSplitter.builder()
                .withChunkSize(ragConfigData.getChunk().getSize())
                .withMinChunkSizeChars(ragConfigData.getChunk().getMinChunkSize())
                .withMinChunkLengthToEmbed(ragConfigData.getChunk().getMinChunkToEmbed())
                .withMaxNumChunks(ragConfigData.getChunk().getMaxNumChunks())
                .withKeepSeparator(ragConfigData.getChunk().isKeepSeparator())
                .build();
    }

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    @Bean("openAIChatClient")
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel,
                                SimpleLoggerAdvisor simpleLoggerAdvisor,
                                SafeGuardAdvisor safeGuardAdvisor,
                                ErrorWrappingAdvisor errorWrappingAdvisor,
                                SystemPromptAdvisor systemPromptAdvisor,
                                ValidationAdvisor validationAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(errorWrappingAdvisor, safeGuardAdvisor, simpleLoggerAdvisor, systemPromptAdvisor, validationAdvisor)
                .build();
    }

    @Bean("openAIGeneralChatClient")
    ChatClient openAIGeneralChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean("openAIChatClientWithMemory")
    ChatClient openAIChatClientWithMemory(OpenAiChatModel openAiChatModel,
                                          ChatMemory chatMemory,
                                          PgVectorStore vectorStore) {
        return ChatClient.builder(openAiChatModel)
//                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory)
//                        .systemPromptTemplate(new PromptTemplate(promptChatMemorySystemPrompt))
//                        .build())
                .defaultAdvisors(VectorStoreChatMemoryAdvisor.builder(vectorStore)
                                .systemPromptTemplate(new PromptTemplate(vectorStoreMemorySystemPrompt))
                                .defaultTopK(TOP_K)
                                .build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean("vertexAIChatClient")
    ChatClient vertexAIChatClient(VertexAiGeminiChatModel vertexAiGeminiChatModel) {
        return ChatClient.builder(vertexAiGeminiChatModel).build();
    }

    @Bean("huggingFaceChatClient")
    ChatClient huggingFaceChatClient(HuggingfaceChatModel huggingfaceChatModel) {
        return ChatClient.builder(huggingfaceChatModel).build();
    }

    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean("openAIRAGChatClient")
    ChatClient openAIRAGChatClient(OpenAiChatModel openAiChatModel,
                                   SimpleVectorStore simpleVectorStore,
                                   SimpleLoggerAdvisor simpleLoggerAdvisor,
                                   RagConfigData ragConfigData) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(simpleVectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(ragConfigData.getTopK())
                                .similarityThreshold(ragConfigData.getSimilarityThreshold())
                                .build()).build(), simpleLoggerAdvisor).build();
    }

    @Bean("openAIAdvancedRAGChatClient")
    ChatClient openAIAdvancedRAGChatClient(OpenAiChatModel openAiChatModel,
                                           RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }

    @Bean("openAIAgentChatClientVision")
    ChatClient openAIAgentChatClientVision(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(ChatOptions.builder()
                        .model(openAIVisionModel)
                        .build())
                .build();
    }

    @Bean("openAIAgentChatClient")
    ChatClient openAIAgentChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory, ContentSanitizerAdvisor contentSanitizerAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), contentSanitizerAdvisor)
                .build();
    }

    @Bean
    @Primary
    EmbeddingModel primaryEmbedding(@Qualifier("openAiEmbeddingModel") EmbeddingModel delegateEmbeddingModel) {
        return delegateEmbeddingModel;
    }

    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    SafeGuardAdvisor safeGuardAdvisor() {
        return new SafeGuardAdvisor(List.of(
                "password", "ssn", "credit card", "iban", "bank account",
                "api_key", "secret", "private_key", "token",
                "confidential", "classified", "internal only", "Ignore previous instructions",
                "Ignore instructions", "system prompt", "hack"));
    }

}
