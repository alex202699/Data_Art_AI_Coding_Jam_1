package com.dataart.ticketing.comment;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.activity.ActivityService;
import com.dataart.ticketing.comment.dto.CommentDtos.CommentResponse;
import com.dataart.ticketing.domain.Comment;
import com.dataart.ticketing.domain.Ticket;
import com.dataart.ticketing.domain.User;
import com.dataart.ticketing.repository.CommentRepository;
import com.dataart.ticketing.repository.TicketRepository;
import com.dataart.ticketing.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final TicketRepository tickets;
    private final UserRepository users;
    private final ActivityService activityService;

    public CommentService(CommentRepository comments, TicketRepository tickets,
                          UserRepository users, ActivityService activityService) {
        this.comments = comments;
        this.tickets = tickets;
        this.users = users;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID ticketId) {
        requireTicket(ticketId);
        return comments.findByTicket_IdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public CommentResponse add(UUID ticketId, UUID currentUserId, String rawBody) {
        String body = requireBody(rawBody);
        Ticket ticket = requireTicket(ticketId);
        User author = users.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        // Adding a comment must NOT touch the ticket's modified_at (board ordering unaffected).
        Comment saved = comments.save(new Comment(ticket, author, body));
        activityService.record(ticket, currentUserId, ActivityService.COMMENT_ADDED, null, null, null);
        return toResponse(saved);
    }

    @Transactional
    public CommentResponse update(UUID commentId, UUID currentUserId, String rawBody) {
        String body = requireBody(rawBody);
        Comment comment = requireOwnComment(commentId, currentUserId, "edit");
        comment.markEdited(body); // does not touch ticket modified_at
        activityService.record(comment.getTicket(), currentUserId, ActivityService.COMMENT_EDITED, null, null, null);
        return toResponse(comment);
    }

    @Transactional
    public void delete(UUID commentId, UUID currentUserId) {
        Comment comment = requireOwnComment(commentId, currentUserId, "delete");
        // Record before deleting so the actor/ticket are captured.
        activityService.record(comment.getTicket(), currentUserId, ActivityService.COMMENT_DELETED, null, null, null);
        comments.delete(comment);
    }

    private Comment requireOwnComment(UUID commentId, UUID currentUserId, String verb) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only " + verb + " your own comments");
        }
        return comment;
    }

    private Ticket requireTicket(UUID ticketId) {
        return tickets.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private static String requireBody(String rawBody) {
        String body = rawBody == null ? "" : rawBody.trim();
        if (body.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }
        return body;
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId().toString(),
                c.getTicket().getId().toString(),
                c.getAuthor().getId().toString(),
                c.getAuthor().getEmail(),
                c.getBody(),
                c.getCreatedAt(),
                c.getEditedAt());
    }
}
