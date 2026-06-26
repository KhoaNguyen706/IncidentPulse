package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.IncidentRequest;
import com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest;
import com.example.IncidentPulse.DTO.Response.IncidentHistoryResponse;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.DTO.Response.PageResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapper;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.IncidentHistory;
import com.example.IncidentPulse.Model.OnCallShift;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.IncidentHistoryRepository;
import com.example.IncidentPulse.Repository.IncidentRepository;
import com.example.IncidentPulse.Repository.IncidentSpecifications;
import com.example.IncidentPulse.Repository.OnCallShiftRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import com.example.IncidentPulse.WebSocket.IncidentEvent;
import com.example.IncidentPulse.WebSocket.IncidentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.IncidentPulse.Security.SecurityUtil.getCurrentUser;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OnCallShiftRepository onCallShiftRepository;
    private final EmailService emailService;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final IncidentEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    /*
     * The incident state machine: a status may only move to the statuses
     * listed in its set. This is the single source of truth for what
     * transitions are legal, which keeps the rule out of the controller and
     * makes it trivial to test.
     *
     *   OPENED        -> INVESTIGATING
     *   INVESTIGATING -> RESOLVED
     *   RESOLVED      -> CLOSED, or back to INVESTIGATING (reopened)
     *   CLOSED        -> (terminal, no further changes)
     */
    private static final Map<Incident.status, Set<Incident.status>> ALLOWED_TRANSITIONS =
            new EnumMap<>(Incident.status.class);

    static {
        ALLOWED_TRANSITIONS.put(Incident.status.OPENED, EnumSet.of(Incident.status.INVESTIGATING));
        ALLOWED_TRANSITIONS.put(Incident.status.INVESTIGATING, EnumSet.of(Incident.status.RESOLVED));
        ALLOWED_TRANSITIONS.put(Incident.status.RESOLVED,
                EnumSet.of(Incident.status.CLOSED, Incident.status.INVESTIGATING));
        ALLOWED_TRANSITIONS.put(Incident.status.CLOSED, EnumSet.noneOf(Incident.status.class));
    }

    @Autowired
    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           UserMapper userMapper,
                           OnCallShiftRepository onCallShiftRepository,
                           EmailService emailService,
                           IncidentHistoryRepository incidentHistoryRepository,
                           IncidentEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.onCallShiftRepository = onCallShiftRepository;
        this.emailService = emailService;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    public User assignUser() {
        OnCallShift onCallShift = onCallShiftRepository.findCurrentOnCallShift(LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.NO_ON_CALL_ENGINEER));

        User assignedUser = onCallShift.getUser_id();
        if (assignedUser == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findById(assignedUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public IncidentResponse createIncident(IncidentRequest incidentRequest) {
        User currentUser = getCurrentUser();
        User assignee = assignUser();

        Incident incident = new Incident();
        incident.setTitle(incidentRequest.getTitle());
        incident.setCreatedBy(currentUser);
        incident.setMessage(incidentRequest.getMessage());
        incident.setAssignedTo(assignee);
        incident.setSeverity(incidentRequest.getSeverity());
        incident.setStatus(Incident.status.OPENED);
        incidentRepository.save(incident);

        // Record the very first entry in the audit trail.
        recordHistory(incident, currentUser, null, Incident.status.OPENED,
                IncidentHistory.ActionType.CREATED, "Incident created");

        try {
            emailService.sendAssignmentEmail(assignee, incident);
        } catch (Exception e) {
            log.warn("Failed to send assignment email: {}", e.getMessage());
        }

        eventPublisher.publish(IncidentEvent.builder()
                .id(incident.getId())
                .type("CREATED")
                .status(incident.getStatus())
                .toStatus(incident.getStatus())
                .actor(currentUser != null ? currentUser.getUsername() : null)
                .occurredAt(LocalDateTime.now())
                .build());

        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getMyIncident(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Incident incident = incidentRepository.findIncidentByAssignedTo(user);
        if (incident == null) {
            throw new AppException(ErrorCode.INCIDENT_NOT_FOUND);
        }
        return toResponse(incident);
    }

    /**
     * Edit the editable fields of an incident (title, severity, message).
     * Status is intentionally NOT changed here - that goes through updateStatus
     * so it is validated by the state machine and recorded in history.
     */
    @Transactional
    public IncidentResponse updateIncident(Long incidentId, IncidentRequest incidentRequest) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        if (incidentRequest.getTitle() != null) incident.setTitle(incidentRequest.getTitle());
        if (incidentRequest.getSeverity() != null) incident.setSeverity(incidentRequest.getSeverity());
        if (incidentRequest.getMessage() != null) incident.setMessage(incidentRequest.getMessage());

        incidentRepository.save(incident);
        return toResponse(incident);
    }

    /**
     * Move an incident from its current status to a new one, but only if the
     * transition is allowed by the state machine. Every successful change is
     * written to the incident_history table. Wrapped in a transaction so the
     * incident update and the history insert either both succeed or both fail.
     */
    @Transactional
    public IncidentResponse updateStatus(Long incidentId, IncidentStatusUpdateRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        Incident.status from = incident.getStatus();
        Incident.status to = request.getStatus();

        if (to == null || !isTransitionAllowed(from, to)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        incident.setStatus(to);
        incidentRepository.save(incident);

        User actor = getCurrentUser();
        recordHistory(incident, actor, from, to, actionTypeFor(to), request.getNote());

        eventPublisher.publish(IncidentEvent.builder()
                .id(incident.getId())
                .type("STATUS_CHANGED")
                .status(to)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor != null ? actor.getUsername() : null)
                .occurredAt(LocalDateTime.now())
                .build());

        return toResponse(incident);
    }

    /**
     * Paginated, filtered listing of incidents. Any combination of the three
     * filters may be supplied (or omitted). Runs inside a read-only transaction
     * so the LAZY {@code createdBy}/{@code assignedTo} associations can be
     * resolved when building each response.
     */
    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> findIncidents(Incident.status status,
                                                        Incident.severity severity,
                                                        String assignee,
                                                        Pageable pageable) {
        Specification<Incident> spec = Specification.allOf(
                IncidentSpecifications.hasStatus(status),
                IncidentSpecifications.hasSeverity(severity),
                IncidentSpecifications.assignedToUsername(assignee));

        Page<Incident> page = incidentRepository.findAll(spec, pageable);
        List<IncidentResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public List<IncidentHistoryResponse> getIncidentHistory(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new AppException(ErrorCode.INCIDENT_NOT_FOUND);
        }
        return incidentHistoryRepository.findHistoryForIncident(incidentId).stream()
                .map(h -> IncidentHistoryResponse.builder()
                        .actionType(h.getActionType())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .actorUsername(h.getActor() != null ? h.getActor().getUsername() : null)
                        .message(h.getMessage())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private boolean isTransitionAllowed(Incident.status from, Incident.status to) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(from, EnumSet.noneOf(Incident.status.class))
                .contains(to);
    }

    private IncidentHistory.ActionType actionTypeFor(Incident.status to) {
        return switch (to) {
            case RESOLVED -> IncidentHistory.ActionType.RESOLVED;
            case CLOSED -> IncidentHistory.ActionType.CLOSED;
            case INVESTIGATING -> IncidentHistory.ActionType.ACKNOWLEDGED;
            default -> IncidentHistory.ActionType.STATUS_CHANGED;
        };
    }

    public void recordHistory(Incident incident, User actor, Incident.status from,
                               Incident.status to, IncidentHistory.ActionType actionType, String message) {
        IncidentHistory history = IncidentHistory.builder()
                .incident(incident)
                .actor(actor)
                .fromStatus(from)
                .toStatus(to)
                .actionType(actionType)
                .message(message)
                .build();
        incidentHistoryRepository.save(history);
    }

    public IncidentResponse toResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .message(incident.getMessage())
                .assignedTo(userMapper.toResponse(incident.getAssignedTo()))
                .createdBy(userMapper.toResponse(incident.getCreatedBy()))
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
