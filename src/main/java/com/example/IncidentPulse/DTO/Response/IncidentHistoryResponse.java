package com.example.IncidentPulse.DTO.Response;

import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.IncidentHistory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentHistoryResponse {

    IncidentHistory.ActionType actionType;
    Incident.status fromStatus;
    Incident.status toStatus;
    String actorUsername;
    String message;
    LocalDateTime createdAt;
}
