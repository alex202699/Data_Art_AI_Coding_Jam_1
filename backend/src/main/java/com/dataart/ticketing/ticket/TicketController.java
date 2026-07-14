package com.dataart.ticketing.ticket;

import java.util.Map;
import java.util.UUID;

import com.dataart.ticketing.activity.ActivityService;
import com.dataart.ticketing.ticket.dto.TicketDtos.CreateTicketRequest;
import com.dataart.ticketing.ticket.dto.TicketDtos.StateRequest;
import com.dataart.ticketing.ticket.dto.TicketDtos.TicketResponse;
import com.dataart.ticketing.ticket.dto.TicketDtos.UpdateTicketRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ActivityService activityService;

    public TicketController(TicketService ticketService, ActivityService activityService) {
        this.ticketService = ticketService;
        this.activityService = activityService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam String teamId) {
        return Map.of("tickets", ticketService.listByTeam(teamId));
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        return Map.of("ticket", ticketService.get(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@AuthenticationPrincipal UUID userId,
                                                       @RequestBody CreateTicketRequest req) {
        TicketResponse ticket = ticketService.create(
                userId, req.teamId(), req.epicId(), req.type(), req.title(), req.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ticket", ticket));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@AuthenticationPrincipal UUID userId,
                                      @PathVariable UUID id, @RequestBody UpdateTicketRequest req) {
        TicketResponse ticket = ticketService.update(
                userId, id, req.teamId(), req.epicId(), req.type(), req.state(), req.title(), req.body());
        return Map.of("ticket", ticket);
    }

    @PatchMapping("/{id}/state")
    public Map<String, Object> updateState(@AuthenticationPrincipal UUID userId,
                                           @PathVariable UUID id, @RequestBody StateRequest req) {
        return Map.of("ticket", ticketService.updateState(userId, id, req.state()));
    }

    @GetMapping("/{id}/activity")
    public Map<String, Object> activity(@PathVariable UUID id) {
        return Map.of("activity", activityService.list(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
