package com.genai.java.spring.chat.advisor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.chat.openai.dto.response.SummarizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ErrorWrappingAdvisor implements CallAdvisor, StreamAdvisor {

    private final ObjectMapper objectMapper;

    public ErrorWrappingAdvisor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("Request received in ErrorWrappingAdvisor with prompt: {}", chatClientRequest.prompt().getUserMessage().getText());
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        String assistantMessage = chatClientResponse.chatResponse().getResult().getOutput().getText().trim();

        if (!assistantMessage.startsWith("```json") && !assistantMessage.startsWith("{")) {
            SummarizationResponse dto = new SummarizationResponse(null, null, assistantMessage);
            try {
                chatClientResponse = chatClientResponse.mutate()
                        .chatResponse(ChatResponse.builder()
                                .generations(List.of(new Generation(new AssistantMessage(objectMapper.writeValueAsString(dto)))))
                                .build())
                        .context(Map.copyOf(chatClientRequest.context()))
                        .build();

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Flux<ChatClientResponse> chatClientResponse = streamAdvisorChain.nextStream(chatClientRequest);

        return chatClientResponse;
    }

    @Override
    public String getName() {
        return "CustomErrorWrappingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
