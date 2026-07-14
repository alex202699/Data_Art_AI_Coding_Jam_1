package com.dataart.ticketing.epic.dto;

import java.time.OffsetDateTime;

/** Request/response payloads for the epic endpoints. */
public final class EpicDtos {

    private EpicDtos() {
    }

    public record CreateEpicRequest(String teamId, String title, String description) {
    }

    public record UpdateEpicRequest(String title, String description) {
    }

    public record EpicResponse(
            String id,
            String teamId,
            String title,
            String description,
            long ticketCount,
            OffsetDateTime createdAt,
            OffsetDateTime modifiedAt) {
    }
}
