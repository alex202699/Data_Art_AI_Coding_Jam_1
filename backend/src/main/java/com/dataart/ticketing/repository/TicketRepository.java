package com.dataart.ticketing.repository;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByTeam_Id(UUID teamId);

    long countByTeam_Id(UUID teamId);

    long countByEpic_Id(UUID epicId);
}
