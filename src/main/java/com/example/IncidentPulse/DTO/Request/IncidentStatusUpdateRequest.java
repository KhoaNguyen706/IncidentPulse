package com.example.IncidentPulse.DTO.Request;

import com.example.IncidentPulse.Model.Incident;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentStatusUpdateRequest {

    // The status we want to move the incident TO.
    Incident.status status;

    // Optional human note explaining the change (saved into the history trail).
    String note;
}
