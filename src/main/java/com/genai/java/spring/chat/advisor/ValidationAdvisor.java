package com.genai.java.spring.chat.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ValidationAdvisor implements CallAdvisor, StreamAdvisor {

    private static final int MAX_TOKENS_INPUT = 10;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        validateInput(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        validateOutput(chatClientResponse);
        return chatClientResponse;
    }

    private static void validateOutput(ChatClientResponse chatClientResponse) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            throw new IllegalArgumentException("LLM returned a null result!");
        }

        String output = chatResponse.getResult().getOutput().getText();

        // Ensure non-empty response
        if (output == null || output.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM returned an empty response!");
        }
    }

    private static void validateInput(ChatClientRequest chatClientRequest) {
        String input = chatClientRequest.prompt().getContents();
        // Length validation
        if (input.length() > MAX_TOKENS_INPUT) {
            //or return a failure response as in the safeguardadvisor
            throw new IllegalArgumentException("Prompt too long: exceeds " + MAX_TOKENS_INPUT + " characters!");
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        validateInput(chatClientRequest);
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "CustomValidationAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
