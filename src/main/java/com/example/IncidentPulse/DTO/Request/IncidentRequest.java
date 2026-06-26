package com.example.IncidentPulse.DTO.Request;

import com.example.IncidentPulse.Model.Incident;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentRequest {

    @NotBlank
    @Size(max = 200)
    String title;

    @NotNull
    Incident.severity severity;

    @NotBlank
    String message;
}
