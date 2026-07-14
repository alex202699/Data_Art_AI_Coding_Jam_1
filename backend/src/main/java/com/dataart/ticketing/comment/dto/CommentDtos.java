package com.dataart.ticketing.comment.dto;

import java.time.OffsetDateTime;

/** Request/response payloads for the comment endpoints. */
public final class CommentDtos {

    private CommentDtos() {
    }

    public record CreateCommentRequest(String body) {
    }

    public record CommentResponse(
            String id,
            String ticketId,
            String authorId,
            String authorEmail,
            String body,
            OffsetDateTime createdAt) {
    }
}
