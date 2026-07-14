package com.dataart.ticketing.epic;

import java.util.Map;
import java.util.UUID;

import com.dataart.ticketing.epic.dto.EpicDtos.CreateEpicRequest;
import com.dataart.ticketing.epic.dto.EpicDtos.EpicResponse;
import com.dataart.ticketing.epic.dto.EpicDtos.UpdateEpicRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/epics")
public class EpicController {

    private final EpicService epicService;

    public EpicController(EpicService epicService) {
        this.epicService = epicService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam String teamId) {
        return Map.of("epics", epicService.listByTeam(teamId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateEpicRequest req) {
        EpicResponse epic = epicService.create(req.teamId(), req.title(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("epic", epic));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody UpdateEpicRequest req) {
        return Map.of("epic", epicService.update(id, req.title(), req.description()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        epicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
