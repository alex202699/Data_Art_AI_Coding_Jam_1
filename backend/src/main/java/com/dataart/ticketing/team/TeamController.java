package com.dataart.ticketing.team;

import java.util.Map;
import java.util.UUID;

import com.dataart.ticketing.team.dto.TeamDtos.TeamRequest;
import com.dataart.ticketing.team.dto.TeamDtos.TeamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("teams", teamService.list());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody TeamRequest req) {
        TeamResponse team = teamService.create(req.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("team", team));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> rename(@PathVariable UUID id, @RequestBody TeamRequest req) {
        return Map.of("team", teamService.rename(id, req.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
