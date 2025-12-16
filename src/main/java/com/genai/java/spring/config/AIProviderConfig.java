package com.genai.java.spring.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIProviderConfig {


    @Bean("openAIChatClient")
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
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



}
