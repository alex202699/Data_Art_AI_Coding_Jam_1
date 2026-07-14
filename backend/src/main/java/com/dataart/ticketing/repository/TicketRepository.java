package com.dataart.ticketing.repository;

import java.util.UUID;

import com.dataart.ticketing.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    long countByTeamId(UUID teamId);

    long countByEpicId(UUID epicId);
}
