package com.dataart.ticketing.epic;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.Epic;
import com.dataart.ticketing.domain.Team;
import com.dataart.ticketing.epic.dto.EpicDtos.EpicResponse;
import com.dataart.ticketing.repository.EpicRepository;
import com.dataart.ticketing.repository.TeamRepository;
import com.dataart.ticketing.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EpicService {

    private final EpicRepository epics;
    private final TeamRepository teams;
    private final TicketRepository tickets;

    public EpicService(EpicRepository epics, TeamRepository teams, TicketRepository tickets) {
        this.epics = epics;
        this.teams = teams;
        this.tickets = tickets;
    }

    @Transactional(readOnly = true)
    public List<EpicResponse> listByTeam(String rawTeamId) {
        UUID teamId = parseTeamId(rawTeamId);
        return epics.findByTeam_IdOrderByModifiedAtDesc(teamId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EpicResponse create(String rawTeamId, String rawTitle, String rawDescription) {
        UUID teamId = parseTeamId(rawTeamId);
        String title = requireTitle(rawTitle, "A valid teamId and non-empty title are required");
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The selected team no longer exists"));
        Epic epic = new Epic(team, title, normalizeDescription(rawDescription));
        return toResponse(epics.save(epic));
    }

    @Transactional
    public EpicResponse update(UUID id, String rawTitle, String rawDescription) {
        String title = requireTitle(rawTitle, "A non-empty title is required");
        String description = normalizeDescription(rawDescription);
        Epic epic = epics.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Epic not found"));
        boolean changed = !title.equals(epic.getTitle())
                || !java.util.Objects.equals(description, epic.getDescription());
        if (changed) { // no-op saves must not advance modified_at
            epic.setTitle(title);
            epic.setDescription(description);
            epic.touch();
        }
        return toResponse(epic);
    }

    @Transactional
    public void delete(UUID id) {
        Epic epic = epics.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Epic not found"));
        if (tickets.countByEpic_Id(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This epic cannot be deleted while tickets reference it");
        }
        epics.delete(epic);
    }

    private static UUID parseTeamId(String raw) {
        try {
            return UUID.fromString(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid teamId is required");
        }
    }

    private static String requireTitle(String raw, String message) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return title;
    }

    /** Blank/whitespace description becomes null (optional field). */
    private static String normalizeDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private EpicResponse toResponse(Epic epic) {
        return new EpicResponse(
                epic.getId().toString(),
                epic.getTeam().getId().toString(),
                epic.getTitle(),
                epic.getDescription(),
                tickets.countByEpic_Id(epic.getId()),
                epic.getCreatedAt(),
                epic.getModifiedAt());
    }
}
