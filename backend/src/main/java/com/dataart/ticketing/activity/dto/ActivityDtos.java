package com.dataart.ticketing.activity.dto;

import java.time.OffsetDateTime;

/** Response payload for ticket activity history. */
public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record ActivityResponse(
            String id,
            String ticketId,
            String actorId,
            String actorEmail,
            String kind,
            String field,
            String oldValue,
            String newValue,
            OffsetDateTime createdAt) {
    }
}
