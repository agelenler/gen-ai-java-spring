package com.genai.java.spring.multimodality.texttospeech;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TicketStatusTools {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initialize 10 mock tickets
        for (int i = 1; i <= 10; i++) {
            String ticketId = "TICKET-" + i;
            Ticket ticket = new Ticket(
                    ticketId,
                    ZonedDateTime.now(),
                    TicketStatus.OPEN,
                    "user-" + i,
                    "User " + i
            );
            tickets.put(ticketId, ticket);
        }
    }

    /**
     * Tool exposed to the LLM for retrieving a tickets's status.
     * Each call advances the ticket to the next status stage.
     */
    @Tool
    public String getTicketStatus(@ToolParam(description = "Ticket Id") String ticketId, @ToolParam(description = "User Id") String userId) {
        Ticket ticket = tickets.get(ticketId);

        if (ticket != null && !ticket.userId().equals(userId)) {
            return "Ticket ID: " + ticketId + " does not belong to User ID: " + userId;
        }

        if (ticket == null) {
            return "No ticket found for ID: " + ticketId;
        }

        // Move to the next status stage
        TicketStatus current = ticket.status();
        TicketStatus next = nextStage(current);

        Ticket updated = new Ticket(
                ticket.ticketId(),
                ticket.ticketDate(),
                next,
                ticket.userId(),
                ticket.userName()
        );
        tickets.put(ticketId, updated);

        return "Ticket " + ticketId + " for " + ticket.userName()
                + " (Date: " + ticket.ticketDate() + ") is currently " + current;
    }

    private TicketStatus nextStage(TicketStatus current) {
        return switch (current) {
            case OPEN -> TicketStatus.WAITING_ON_CUSTOMER;
            case WAITING_ON_CUSTOMER -> TicketStatus.PROCESSING;
            case PROCESSING -> TicketStatus.RESOLVED;
            case RESOLVED -> TicketStatus.RESOLVED; // stay at final stage
        };
    }
}
