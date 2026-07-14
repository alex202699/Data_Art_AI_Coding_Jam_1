package com.dataart.ticketing.ticket.dto;

import java.time.OffsetDateTime;

/** Request/response payloads for the ticket endpoints. */
public final class TicketDtos {

    private TicketDtos() {
    }

    /** Full create payload. */
    public record CreateTicketRequest(
            String teamId,
            String epicId,
            String type,
            String title,
            String body) {
    }

    /** Full edit payload (includes state). */
    public record UpdateTicketRequest(
            String teamId,
            String epicId,
            String type,
            String state,
            String title,
            String body) {
    }

    /** State-only change (drag-and-drop). */
    public record StateRequest(String state) {
    }

    public record TicketResponse(
            String id,
            String teamId,
            String epicId,
            String epicTitle,
            String type,
            String state,
            String title,
            String body,
            String createdBy,
            String createdByEmail,
            OffsetDateTime createdAt,
            OffsetDateTime modifiedAt) {
    }
}
