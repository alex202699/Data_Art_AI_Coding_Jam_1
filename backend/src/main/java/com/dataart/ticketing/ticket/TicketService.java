package com.dataart.ticketing.ticket;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.dataart.ticketing.domain.Epic;
import com.dataart.ticketing.domain.Team;
import com.dataart.ticketing.domain.Ticket;
import com.dataart.ticketing.domain.User;
import com.dataart.ticketing.repository.EpicRepository;
import com.dataart.ticketing.repository.TeamRepository;
import com.dataart.ticketing.repository.TicketRepository;
import com.dataart.ticketing.repository.UserRepository;
import com.dataart.ticketing.ticket.dto.TicketDtos.TicketResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    static final Set<String> TYPES = Set.of("bug", "feature", "fix");
    static final Set<String> STATES = Set.of(
            "new", "ready_for_implementation", "in_progress", "ready_for_acceptance", "done");

    private final TicketRepository tickets;
    private final TeamRepository teams;
    private final EpicRepository epics;
    private final UserRepository users;

    public TicketService(TicketRepository tickets, TeamRepository teams,
                         EpicRepository epics, UserRepository users) {
        this.tickets = tickets;
        this.teams = teams;
        this.epics = epics;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listByTeam(String rawTeamId) {
        UUID teamId = parseUuid(rawTeamId, "A valid teamId query parameter is required");
        return tickets.findByTeam_Id(teamId).stream()
                // Most-recently-modified first (the board re-groups by column).
                .sorted(Comparator.comparing(Ticket::getModifiedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public TicketResponse create(UUID currentUserId, String rawTeamId, String rawEpicId,
                                 String rawType, String rawTitle, String rawBody) {
        UUID teamId = parseUuid(rawTeamId, "A valid team is required");
        String type = requireEnum(rawType, TYPES, "Type must be one of bug, feature, fix");
        String title = requireText(rawTitle, "Title is required");
        String body = requireText(rawBody, "Body is required");

        Team team = teams.findById(teamId)
                .orElseThrow(() -> badRequest("The selected team no longer exists"));
        Epic epic = resolveEpic(rawEpicId, team);
        User creator = users.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Ticket ticket = new Ticket(team, epic, type, "new", title, body, creator);
        return toResponse(tickets.save(ticket));
    }

    @Transactional
    public TicketResponse update(UUID id, String rawTeamId, String rawEpicId, String rawType,
                                 String rawState, String rawTitle, String rawBody) {
        UUID teamId = parseUuid(rawTeamId, "A valid team is required");
        String type = requireEnum(rawType, TYPES, "Type must be one of bug, feature, fix");
        String state = requireEnum(rawState, STATES, "A valid state is required");
        String title = requireText(rawTitle, "Title is required");
        String body = requireText(rawBody, "Body is required");

        Ticket ticket = require(id);
        Team team = teams.findById(teamId)
                .orElseThrow(() -> badRequest("The selected team no longer exists"));
        Epic epic = resolveEpic(rawEpicId, team);

        boolean changed = !team.getId().equals(ticket.getTeam().getId())
                || !Objects.equals(epicId(epic), epicId(ticket.getEpic()))
                || !type.equals(ticket.getType())
                || !state.equals(ticket.getState())
                || !title.equals(ticket.getTitle())
                || !body.equals(ticket.getBody());

        if (changed) { // no-op saves must not advance modified_at
            ticket.setTeam(team);
            ticket.setEpic(epic);
            ticket.setType(type);
            ticket.setState(state);
            ticket.setTitle(title);
            ticket.setBody(body);
            ticket.touch();
        }
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateState(UUID id, String rawState) {
        String state = requireEnum(rawState, STATES, "A valid state is required");
        Ticket ticket = require(id);
        if (!state.equals(ticket.getState())) {
            ticket.setState(state);
            ticket.touch();
        }
        return toResponse(ticket);
    }

    @Transactional
    public void delete(UUID id) {
        Ticket ticket = require(id);
        tickets.delete(ticket); // comments cascade via FK ON DELETE CASCADE
    }

    private Ticket require(UUID id) {
        return tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    /** Resolve + validate an optional epic against the ticket's team. */
    private Epic resolveEpic(String rawEpicId, Team team) {
        if (rawEpicId == null || rawEpicId.isBlank()) {
            return null;
        }
        UUID epicId = parseUuid(rawEpicId, "A valid epic id is required");
        Epic epic = epics.findById(epicId)
                .orElseThrow(() -> badRequest("The selected epic no longer exists"));
        if (!epic.getTeam().getId().equals(team.getId())) {
            throw badRequest("The selected epic belongs to a different team");
        }
        return epic;
    }

    private static UUID epicId(Epic epic) {
        return epic == null ? null : epic.getId();
    }

    private static UUID parseUuid(String raw, String message) {
        try {
            return UUID.fromString(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            throw badRequest(message);
        }
    }

    private static String requireEnum(String raw, Set<String> allowed, String message) {
        String v = raw == null ? "" : raw.trim();
        if (!allowed.contains(v)) {
            throw badRequest(message);
        }
        return v;
    }

    private static String requireText(String raw, String message) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw badRequest(message);
        }
        return v;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private TicketResponse toResponse(Ticket t) {
        Epic epic = t.getEpic();
        return new TicketResponse(
                t.getId().toString(),
                t.getTeam().getId().toString(),
                epic == null ? null : epic.getId().toString(),
                epic == null ? null : epic.getTitle(),
                t.getType(),
                t.getState(),
                t.getTitle(),
                t.getBody(),
                t.getCreatedBy().getId().toString(),
                t.getCreatedBy().getEmail(),
                t.getCreatedAt(),
                t.getModifiedAt());
    }
}
