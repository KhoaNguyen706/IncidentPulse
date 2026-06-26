package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.IncidentRequest;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapper;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.OnCallShift;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.IncidentRepository;
import com.example.IncidentPulse.Repository.OnCallShiftRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import com.example.IncidentPulse.Security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for IncidentService.
 *
 * The tricky part: createIncident() reads the logged-in user via the STATIC
 * method SecurityUtil.getCurrentUser(). Static calls can't be replaced with a
 * normal @Mock, so we use Mockito.mockStatic(...) inside a try-with-resources
 * block. The static stub is only active inside that block, which keeps the
 * override from leaking into other tests.
 */
@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OnCallShiftRepository onCallShiftRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.example.IncidentPulse.Repository.IncidentHistoryRepository incidentHistoryRepository;

    @Mock
    private com.example.IncidentPulse.WebSocket.IncidentEventPublisher eventPublisher;

    @InjectMocks
    private IncidentService incidentService;

    private User userWithId(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private OnCallShift shiftFor(User user) {
        OnCallShift shift = new OnCallShift();
        shift.setUser_id(user);
        shift.setStartedAt(LocalDateTime.now().minusHours(1));
        shift.setEndAt(LocalDateTime.now().plusHours(1));
        return shift;
    }

    // ---------------------------------------------------------------------
    // assignUser
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("assignUser: returns the on-call engineer for the current shift")
    void assignUser_returnsOnCallEngineer() {
        User engineer = userWithId(7L, "eng");
        when(onCallShiftRepository.findCurrentOnCallShift(any(LocalDateTime.class)))
                .thenReturn(Optional.of(shiftFor(engineer)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(engineer));

        User result = incidentService.assignUser();

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getUsername()).isEqualTo("eng");
    }

    @Test
    @DisplayName("assignUser: throws NO_ON_CALL_ENGINEER when there is no active shift")
    void assignUser_throwsWhenNoShift() {
        when(onCallShiftRepository.findCurrentOnCallShift(any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.assignUser())
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NO_ON_CALL_ENGINEER);
    }

    // ---------------------------------------------------------------------
    // createIncident
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("createIncident: creates an OPENED incident assigned to the on-call engineer")
    void createIncident_createsAndAssigns() {
        User reporter = userWithId(1L, "reporter");
        User engineer = userWithId(7L, "eng");

        IncidentRequest request = IncidentRequest.builder()
                .title("Database down")
                .severity(Incident.severity.SEV1)
                .message("Primary DB not responding")
                .build();

        when(onCallShiftRepository.findCurrentOnCallShift(any(LocalDateTime.class)))
                .thenReturn(Optional.of(shiftFor(engineer)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(engineer));
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().username("eng").build());

        // Activate the static stub only for this block.
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(reporter);

            IncidentResponse result = incidentService.createIncident(request);

            assertThat(result.getTitle()).isEqualTo("Database down");
            assertThat(result.getSeverity()).isEqualTo(Incident.severity.SEV1);
            assertThat(result.getStatus()).isEqualTo(Incident.status.OPENED);
            assertThat(result.getMessage()).isEqualTo("Primary DB not responding");
        }

        // The incident must be persisted and an assignment email attempted.
        verify(incidentRepository).save(any(Incident.class));
        verify(emailService).sendAssignmentEmail(any(User.class), any(Incident.class));
    }

    @Test
    @DisplayName("createIncident: still succeeds even if sending the email fails")
    void createIncident_succeedsWhenEmailFails() {
        User reporter = userWithId(1L, "reporter");
        User engineer = userWithId(7L, "eng");

        IncidentRequest request = IncidentRequest.builder()
                .title("Cache outage")
                .severity(Incident.severity.SEV3)
                .message("Redis unreachable")
                .build();

        when(onCallShiftRepository.findCurrentOnCallShift(any(LocalDateTime.class)))
                .thenReturn(Optional.of(shiftFor(engineer)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(engineer));
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().username("eng").build());
        // Simulate the mail server being down.
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendAssignmentEmail(any(User.class), any(Incident.class));

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(reporter);

            // The service swallows email failures, so this must NOT throw.
            IncidentResponse result = incidentService.createIncident(request);

            assertThat(result.getStatus()).isEqualTo(Incident.status.OPENED);
        }

        verify(incidentRepository).save(any(Incident.class));
    }

    // ---------------------------------------------------------------------
    // getMyIncident
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getMyIncident: returns the incident assigned to the current user")
    void getMyIncident_returnsAssignedIncident() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = userWithId(7L, "eng");

        Incident incident = Incident.builder()
                .title("Disk full")
                .severity(Incident.severity.SEV2)
                .status(Incident.status.INVESTIGATING)
                .message("Disk at 98%")
                .createdBy(userWithId(1L, "reporter"))
                .assignedTo(user)
                .build();

        when(authentication.getName()).thenReturn("eng");
        when(userRepository.findUserByUsername("eng")).thenReturn(Optional.of(user));
        when(incidentRepository.findIncidentByAssignedTo(user)).thenReturn(incident);
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().username("reporter").build());

        IncidentResponse result = incidentService.getMyIncident(authentication);

        assertThat(result.getTitle()).isEqualTo("Disk full");
        assertThat(result.getStatus()).isEqualTo(Incident.status.INVESTIGATING);
        assertThat(result.getSeverity()).isEqualTo(Incident.severity.SEV2);
    }

    @Test
    @DisplayName("getMyIncident: throws USER_NOT_FOUND when the principal has no record")
    void getMyIncident_throwsWhenUserMissing() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getMyIncident(authentication))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ---------------------------------------------------------------------
    // updateStatus
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("updateStatus: allows OPENED -> INVESTIGATING and records history")
    void updateStatus_allowsValidTransition() {
        Incident incident = Incident.builder()
                .id(5L)
                .title("API errors")
                .status(Incident.status.OPENED)
                .severity(Incident.severity.SEV2)
                .build();

        com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest request =
                com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest.builder()
                        .status(Incident.status.INVESTIGATING)
                        .note("Looking into it")
                        .build();

        when(incidentRepository.findById(5L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(userWithId(7L, "eng"));

            IncidentResponse result = incidentService.updateStatus(5L, request);

            assertThat(result.getStatus()).isEqualTo(Incident.status.INVESTIGATING);
        }

        // The status change must be written to the audit trail exactly once.
        verify(incidentHistoryRepository).save(any(com.example.IncidentPulse.Model.IncidentHistory.class));
    }

    @Test
    @DisplayName("updateStatus: rejects an illegal transition (OPENED -> CLOSED) and writes no history")
    void updateStatus_rejectsIllegalTransition() {
        Incident incident = Incident.builder()
                .id(5L)
                .status(Incident.status.OPENED)
                .build();

        com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest request =
                com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest.builder()
                        .status(Incident.status.CLOSED)
                        .build();

        when(incidentRepository.findById(5L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.updateStatus(5L, request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        verify(incidentHistoryRepository, org.mockito.Mockito.never())
                .save(any(com.example.IncidentPulse.Model.IncidentHistory.class));
        verify(incidentRepository, org.mockito.Mockito.never()).save(any(Incident.class));
    }

    @Test
    @DisplayName("updateStatus: throws INCIDENT_NOT_FOUND when the id does not exist")
    void updateStatus_throwsWhenIncidentMissing() {
        when(incidentRepository.findById(404L)).thenReturn(Optional.empty());

        com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest request =
                com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest.builder()
                        .status(Incident.status.INVESTIGATING)
                        .build();

        assertThatThrownBy(() -> incidentService.updateStatus(404L, request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INCIDENT_NOT_FOUND);
    }

    // ---------------------------------------------------------------------
    // getIncidentHistory
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getIncidentHistory: throws INCIDENT_NOT_FOUND when incident does not exist")
    void getIncidentHistory_throwsWhenMissing() {
        when(incidentRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> incidentService.getIncidentHistory(404L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INCIDENT_NOT_FOUND);
    }
}
