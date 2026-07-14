package com.dataart.ticketing.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "epics")
public class Epic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The team is chosen at creation and never changes (moving epics between teams is out of scope).
    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false, updatable = false)
    private Team team;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "modified_at", nullable = false)
    private OffsetDateTime modifiedAt;

    protected Epic() {
    }

    public Epic(Team team, String title, String description) {
        this.team = team;
        this.title = title;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.modifiedAt = now;
    }

    /** Explicit, conditional touch — modified_at advances only on a real change. */
    public void touch() {
        this.modifiedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getModifiedAt() {
        return modifiedAt;
    }
}
