package com.example.IncidentPulse.DTO.Request;

import com.example.IncidentPulse.Model.Incident;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Payload posted by external monitors (UptimeRobot, Grafana, Prometheus
 * Alertmanager, ...) to {@code POST /api/v1/webhook/alert}.
 *
 * {@code source} + {@code externalId} identify the originating alert so repeat
 * deliveries are deduplicated. {@code status = "resolved"} auto-resolves the
 * matching incident.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookAlertRequest {

    @NotBlank
    @Size(max = 100)
    String source;

    // Monitor-side identifier for the alert; enables deduplication. Optional.
    @Size(max = 200)
    String externalId;

    @NotBlank
    @Size(max = 200)
    String title;

    @NotNull
    Incident.severity severity;

    String message;

    // Optional. "resolved" auto-resolves the matching incident.
    String status;
}
