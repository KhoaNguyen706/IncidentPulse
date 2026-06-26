package com.example.IncidentPulse.WebSocket;

import com.example.IncidentPulse.Model.Incident;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * What gets pushed to subscribers when an incident is created or changes status.
 * Plain data so it serializes cleanly to JSON over STOMP.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentEvent {
    Long id;
    String type;            // "CREATED" or "STATUS_CHANGED"
    Incident.status status;
    Incident.status fromStatus;
    Incident.status toStatus;
    String actor;           // username that triggered the event, if known
    LocalDateTime occurredAt;
}
