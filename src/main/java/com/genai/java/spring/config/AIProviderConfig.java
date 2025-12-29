package com.genai.java.spring.config;

import com.genai.java.spring.chat.advisor.ErrorWrappingAdvisor;
import com.genai.java.spring.chat.advisor.SystemPromptAdvisor;
import com.genai.java.spring.chat.advisor.ValidationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private static final int MAX_MESSAGES = 5;

    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate,
                                       @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)
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
                                          PgVectorStore pgVectorStore) {
        return ChatClient.builder(openAiChatModel)
//                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory)
//                        .systemPromptTemplate(new PromptTemplate(promptChatMemorySystemPrompt))
//                        .build())
                .defaultAdvisors(VectorStoreChatMemoryAdvisor.builder(pgVectorStore)
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
