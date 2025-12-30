package com.genai.java.spring.multimodality.texttospeech;

import java.time.ZonedDateTime;

public record Ticket(
        String ticketId,
        ZonedDateTime ticketDate,
        TicketStatus status,
        String userId,
        String userName
) {
}

