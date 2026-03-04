package com.genai.java.spring.aiagent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContentSanitizerAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Prompt sanitizedPrompt = sanitizeInput(chatClientRequest);
        chatClientRequest = chatClientRequest.mutate().prompt(sanitizedPrompt).build();
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Prompt sanitizedPrompt = sanitizeInput(chatClientRequest);
        chatClientRequest = chatClientRequest.mutate().prompt(sanitizedPrompt).build();
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "CustomContentSanitizerAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Prompt sanitizeInput(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();
        List<Message> sanitized = new ArrayList<>();
        for (Message message : prompt.getInstructions()) {
            if (message instanceof SystemMessage systemMessage) {
                sanitized.add(new SystemMessage(replaceNullWithEmptyString(systemMessage.getText())));
            } else if (message instanceof UserMessage userMessage) {
                sanitized.add(new UserMessage(replaceNullWithEmptyString(userMessage.getText())));
            } else if (message instanceof AssistantMessage assistantMessage) {
                // preserve tool calls, fix content
                sanitized.add(AssistantMessage.builder()
                        .content(replaceNullWithEmptyString(assistantMessage.getText()))
                        .properties(assistantMessage.getMetadata())
                        .toolCalls(assistantMessage.getToolCalls())
                        .media(assistantMessage.getMedia())
                        .build());
            } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                sanitized.add(ToolResponseMessage.builder()
                        .responses(toolResponseMessage.getResponses())
                        .metadata(toolResponseMessage.getMetadata())
                        .build());
            } else {
                // fallback: try to keep message but never null text
                sanitized.add(message); // or wrap similarly if your Spring AI version has other subtypes
            }
        }
        return new Prompt(sanitized, prompt.getOptions());
    }

    private static String replaceNullWithEmptyString(String text) {
        return text == null ? "" : text;
    }
}
