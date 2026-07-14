package com.dataart.ticketing.ticket;

import java.util.Map;
import java.util.UUID;

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

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
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
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody UpdateTicketRequest req) {
        TicketResponse ticket = ticketService.update(
                id, req.teamId(), req.epicId(), req.type(), req.state(), req.title(), req.body());
        return Map.of("ticket", ticket);
    }

    @PatchMapping("/{id}/state")
    public Map<String, Object> updateState(@PathVariable UUID id, @RequestBody StateRequest req) {
        return Map.of("ticket", ticketService.updateState(id, req.state()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
