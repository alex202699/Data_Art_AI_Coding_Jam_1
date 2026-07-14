package com.dataart.ticketing.repository;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.Epic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpicRepository extends JpaRepository<Epic, UUID> {

    /** Epics for a team, most-recently-modified first. */
    List<Epic> findByTeam_IdOrderByModifiedAtDesc(UUID teamId);

    long countByTeam_Id(UUID teamId);
}
