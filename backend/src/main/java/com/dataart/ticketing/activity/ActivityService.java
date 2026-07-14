package com.dataart.ticketing.activity;

import java.util.List;
import java.util.UUID;

import com.dataart.ticketing.activity.dto.ActivityDtos.ActivityResponse;
import com.dataart.ticketing.domain.Ticket;
import com.dataart.ticketing.domain.TicketActivity;
import com.dataart.ticketing.domain.User;
import com.dataart.ticketing.repository.TicketActivityRepository;
import com.dataart.ticketing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records and reads ticket activity. record() runs within the caller's transaction. */
@Service
public class ActivityService {

    public static final String CREATED = "created";
    public static final String FIELD_CHANGED = "field_changed";
    public static final String STATE_CHANGED = "state_changed";
    public static final String COMMENT_ADDED = "comment_added";
    public static final String COMMENT_EDITED = "comment_edited";
    public static final String COMMENT_DELETED = "comment_deleted";

    private final TicketActivityRepository activity;
    private final UserRepository users;

    public ActivityService(TicketActivityRepository activity, UserRepository users) {
        this.activity = activity;
        this.users = users;
    }

    public void record(Ticket ticket, UUID actorId, String kind,
                       String field, String oldValue, String newValue) {
        User actor = users.getReferenceById(actorId);
        activity.save(new TicketActivity(ticket, actor, kind, field, oldValue, newValue));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(UUID ticketId) {
        return activity.findByTicket_IdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse).toList();
    }

    private ActivityResponse toResponse(TicketActivity a) {
        return new ActivityResponse(
                a.getId().toString(),
                a.getTicket().getId().toString(),
                a.getActor().getId().toString(),
                a.getActor().getEmail(),
                a.getKind(),
                a.getField(),
                a.getOldValue(),
                a.getNewValue(),
                a.getCreatedAt());
    }
}
