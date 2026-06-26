package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.WebhookAlertRequest;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.IncidentHistory;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Turns inbound monitor alerts into incidents.
 *
 * Rules:
 *  - Deduplication: an alert carrying a (source, externalId) that already maps
 *    to a non-CLOSED incident returns that incident instead of creating a new one.
 *  - Auto-resolve: status="resolved" moves the matching incident to RESOLVED.
 *  - New alerts create an OPENED incident, auto-assigned to the on-call engineer
 *    (best-effort: if nobody is on call the incident is still recorded, unassigned).
 *
 * Reuses {@link IncidentService} for on-call assignment, history writes and
 * response mapping so the incident lifecycle stays defined in one place.
 */
@Service
public class WebhookService {

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    public WebhookService(IncidentRepository incidentRepository,
                          IncidentService incidentService,
                          EmailService emailService) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.emailService = emailService;
    }

    @Transactional
    public IncidentResponse ingestAlert(WebhookAlertRequest request) {
        Optional<Incident> existing = findExisting(request);
        boolean resolveRequested = request.getStatus() != null
                && "resolved".equalsIgnoreCase(request.getStatus().trim());

        if (resolveRequested && existing.isPresent()) {
            return autoResolve(existing.get(), request);
        }

        if (existing.isPresent() && existing.get().getStatus() != Incident.status.CLOSED) {
            log.info("Webhook dedup: returning existing incident {} for {}/{}",
                    existing.get().getId(), request.getSource(), request.getExternalId());
            return incidentService.toResponse(existing.get());
        }

        return createFromWebhook(request);
    }

    private Optional<Incident> findExisting(WebhookAlertRequest request) {
        if (request.getExternalId() == null || request.getExternalId().isBlank()) {
            return Optional.empty();
        }
        return incidentRepository.findFirstBySourceAndExternalId(request.getSource(), request.getExternalId());
    }

    private IncidentResponse createFromWebhook(WebhookAlertRequest request) {
        User assignee = null;
        try {
            assignee = incidentService.assignUser();
        } catch (AppException e) {
            log.warn("No on-call engineer for webhook alert from {}: {}", request.getSource(), e.getMessage());
        }

        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setMessage(request.getMessage());
        incident.setSeverity(request.getSeverity());
        incident.setStatus(Incident.status.OPENED);
        incident.setAssignedTo(assignee);
        incident.setSource(request.getSource());
        incident.setExternalId(request.getExternalId());
        incidentRepository.save(incident);

        incidentService.recordHistory(incident, null, null, Incident.status.OPENED,
                IncidentHistory.ActionType.CREATED, "Created via webhook from " + request.getSource());

        if (assignee != null) {
            try {
                emailService.sendAssignmentEmail(assignee, incident);
            } catch (Exception e) {
                log.warn("Failed to send webhook assignment email: {}", e.getMessage());
            }
        }
        return incidentService.toResponse(incident);
    }

    /**
     * Automated resolution from a monitor's "recovered" signal. This is a
     * deliberate, separate path from the human-driven {@code updateStatus}
     * state machine: a monitor reporting recovery should resolve the incident
     * regardless of whether an engineer had acknowledged it yet.
     */
    private IncidentResponse autoResolve(Incident incident, WebhookAlertRequest request) {
        if (incident.getStatus() == Incident.status.RESOLVED
                || incident.getStatus() == Incident.status.CLOSED) {
            return incidentService.toResponse(incident);
        }
        Incident.status from = incident.getStatus();
        incident.setStatus(Incident.status.RESOLVED);
        incidentRepository.save(incident);

        incidentService.recordHistory(incident, null, from, Incident.status.RESOLVED,
                IncidentHistory.ActionType.RESOLVED, "Auto-resolved via webhook from " + request.getSource());
        return incidentService.toResponse(incident);
    }
}
