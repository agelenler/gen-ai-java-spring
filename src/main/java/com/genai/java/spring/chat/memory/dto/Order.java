package com.genai.java.spring.chat.memory.dto;

public record Order(
        String orderId,
        String carrier,
        OrderStatus status,
        String userId,
        String userName
) {
}
