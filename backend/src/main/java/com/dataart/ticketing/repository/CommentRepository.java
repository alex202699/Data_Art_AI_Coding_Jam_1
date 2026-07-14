package com.dataart.ticketing.repository;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /** Comments for a ticket, oldest first. */
    List<Comment> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
