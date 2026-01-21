package com.example.IncidentPulse.DTO.Response;


import com.example.IncidentPulse.Model.Incident;
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
public class IncidentResponse {
    String title;
    UserResponse createdBy;
    UserResponse assignedTo;
    Incident.status status;
    Incident.severity severity;
    String message;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
