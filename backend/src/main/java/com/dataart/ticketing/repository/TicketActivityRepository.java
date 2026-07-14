package com.dataart.ticketing.repository;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.TicketActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketActivityRepository extends JpaRepository<TicketActivity, UUID> {

    /** Activity for a ticket, oldest first. */
    List<TicketActivity> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
