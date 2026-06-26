package com.example.IncidentPulse.Controller;

import com.example.IncidentPulse.DTO.Request.WebhookAlertRequest;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.Service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Ingestion endpoint for external monitors. Authentication is by the
 * {@code X-API-Key} header, enforced by {@code ApiKeyFilter} (no JWT), so this
 * path is permitted in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/alert")
    public ApiResponse<IncidentResponse> ingestAlert(@Valid @RequestBody WebhookAlertRequest request) {
        IncidentResponse incident = webhookService.ingestAlert(request);
        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .success(true)
                .now(LocalDateTime.now())
                .data(incident)
                .message("Alert ingested")
                .build();
    }
}
