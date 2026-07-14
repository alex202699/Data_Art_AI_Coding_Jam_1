package com.dataart.ticketing.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Minimal mapping of the tickets table — only the columns needed to count tickets by
 * team / epic (for the referenced-delete guards and the Teams/Epics count columns).
 * Full ticket CRUD is a later feature; this entity intentionally maps just the keys.
 * Hibernate {@code ddl-auto: validate} allows unmapped columns.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "epic_id")
    private UUID epicId;

    protected Ticket() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public UUID getEpicId() {
        return epicId;
    }
}
