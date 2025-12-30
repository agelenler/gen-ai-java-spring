package com.genai.java.spring.multimodality.texttospeech;

public record TicketResponse(
        String ticketId,
        TicketStatus status,
        String message
) {
}

