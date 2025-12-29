package com.genai.java.spring.chat.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/orders/chat")
public class OrderController {
    private final ChatClient chatClient;
    private final OrderStatusTools orderStatusTools;
    private final ChatMemory chatMemory;

    private final static String SYSTEM_PROMPT = "You are Order Helper, a virtual assistant that helps users track their orders." +
            "Your tasks:" +
            "- Assist the user with checking the shipping status of their orders." +
            "- If the user provides an order ID or if you can find the order ID from chat memory, use the available tool `getOrderStatus(orderId)` to fetch the current status." +
            "- If the user asks about something unrelated to orders (e.g., weather, jokes, general chit-chat), politely refuse and remind them you can only help with order tracking." +
            "Guidelines:" +
            "- Do not make up order statuses; only use the tool for that information." +
            "- If you can find the order id from chat memory use it to answer directly, otherwise politely ask the user to provide it." +
            "- Keep responses short, clear, and customer-friendly. Respond by naming the customer and asking for further helps.";


    public OrderController(@Qualifier("openAIChatClientWithMemory") ChatClient chatClient,
                           OrderStatusTools orderStatusTools,
                           ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.orderStatusTools = orderStatusTools;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/status")
    public String orderStatus(@RequestBody String message, @RequestHeader(value = "user-id") String userId) {
        log.info("Chat memory for current user: {}", userId);
        chatMemory.get(userId).forEach(m ->
                log.info("{}: {}", m.getMessageType(), m.getMetadata()));

        String userMessage = message + ". user-id: " + userId;

        return chatClient.prompt()
                .tools(orderStatusTools)
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("User query: {userMessage}").param("userMessage", userMessage))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();
    }
}
