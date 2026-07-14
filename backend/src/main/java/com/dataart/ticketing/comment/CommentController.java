package com.dataart.ticketing.comment;

import java.util.Map;
import java.util.UUID;

import com.dataart.ticketing.comment.dto.CommentDtos.CommentResponse;
import com.dataart.ticketing.comment.dto.CommentDtos.CreateCommentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Map<String, Object> list(@PathVariable UUID ticketId) {
        return Map.of("comments", commentService.list(ticketId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> add(@PathVariable UUID ticketId,
                                                    @AuthenticationPrincipal UUID userId,
                                                    @RequestBody CreateCommentRequest req) {
        CommentResponse comment = commentService.add(ticketId, userId, req.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("comment", comment));
    }
}
