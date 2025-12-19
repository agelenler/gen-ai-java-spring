package com.genai.java.spring.chat.openai;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAIGeneralChatController {

    private final ChatClient chatClient;

    public OpenAIGeneralChatController(@Qualifier("openAIGeneralChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/general-chat")
    public String generalChat(@RequestBody String message) {
        ChatOptions chatOptions = ChatOptions.builder()
                .temperature(2.0)
                .topP(0.1)
                .build();
        return chatClient.prompt()
                .options(chatOptions)
                .user(message)
                .call()
                .content();
    }

}
