package com.genai.java.spring.config;

import com.genai.java.spring.chat.advisor.ErrorWrappingAdvisor;
import com.genai.java.spring.chat.advisor.SystemPromptAdvisor;
import com.genai.java.spring.chat.advisor.ValidationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AIProviderConfig {


    @Bean("openAIChatClient")
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel,
                                SimpleLoggerAdvisor simpleLoggerAdvisor,
                                SafeGuardAdvisor safeGuardAdvisor,
                                ErrorWrappingAdvisor errorWrappingAdvisor,
                                SystemPromptAdvisor systemPromptAdvisor,
                                ValidationAdvisor validationAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(safeGuardAdvisor, simpleLoggerAdvisor, errorWrappingAdvisor, systemPromptAdvisor)
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
                "system prompt", "hack"
        ));
    }


}
