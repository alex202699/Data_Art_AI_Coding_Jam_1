package com.dataart.ticketing.team;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.domain.Team;
import com.dataart.ticketing.repository.EpicRepository;
import com.dataart.ticketing.repository.TeamRepository;
import com.dataart.ticketing.repository.TicketRepository;
import com.dataart.ticketing.team.dto.TeamDtos.TeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final TeamRepository teams;
    private final EpicRepository epics;
    private final TicketRepository tickets;

    public TeamService(TeamRepository teams, EpicRepository epics, TicketRepository tickets) {
        this.teams = teams;
        this.epics = epics;
        this.tickets = tickets;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> list() {
        return teams.findAllOrderByNameCi().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TeamResponse create(String rawName) {
        String name = requireName(rawName);
        if (teams.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team with that name already exists");
        }
        return toResponse(teams.save(new Team(name)));
    }

    @Transactional
    public TeamResponse rename(UUID id, String rawName) {
        String name = requireName(rawName);
        Team team = teams.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        if (teams.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team with that name already exists");
        }
        if (!name.equals(team.getName())) { // no-op saves must not advance modified_at
            team.setName(name);
            team.touch();
        }
        return toResponse(team);
    }

    @Transactional
    public void delete(UUID id) {
        Team team = teams.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
        if (epics.countByTeam_Id(id) > 0 || tickets.countByTeamId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This team cannot be deleted while it has tickets or epics");
        }
        teams.delete(team);
    }

    private static String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty team name is required");
        }
        return name;
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(
                team.getId().toString(),
                team.getName(),
                tickets.countByTeamId(team.getId()),
                epics.countByTeam_Id(team.getId()),
                team.getCreatedAt(),
                team.getModifiedAt());
    }
}
