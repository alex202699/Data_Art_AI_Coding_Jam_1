package com.dataart.ticketing.comment;

import java.util.List;
import java.util.UUID;

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

    public CommentService(CommentRepository comments, TicketRepository tickets, UserRepository users) {
        this.comments = comments;
        this.tickets = tickets;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID ticketId) {
        requireTicket(ticketId);
        return comments.findByTicket_IdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public CommentResponse add(UUID ticketId, UUID currentUserId, String rawBody) {
        String body = rawBody == null ? "" : rawBody.trim();
        if (body.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }
        Ticket ticket = requireTicket(ticketId);
        User author = users.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        // Adding a comment must NOT touch the ticket's modified_at (board ordering unaffected).
        return toResponse(comments.save(new Comment(ticket, author, body)));
    }

    private Ticket requireTicket(UUID ticketId) {
        return tickets.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId().toString(),
                c.getTicket().getId().toString(),
                c.getAuthor().getId().toString(),
                c.getAuthor().getEmail(),
                c.getBody(),
                c.getCreatedAt());
    }
}
