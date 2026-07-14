package com.dataart.ticketing.team.dto;

import java.time.OffsetDateTime;

/** Request/response payloads for the team endpoints. */
public final class TeamDtos {

    private TeamDtos() {
    }

    public record TeamRequest(String name) {
    }

    public record TeamResponse(
            String id,
            String name,
            long ticketCount,
            long epicCount,
            OffsetDateTime createdAt,
            OffsetDateTime modifiedAt) {
    }
}
