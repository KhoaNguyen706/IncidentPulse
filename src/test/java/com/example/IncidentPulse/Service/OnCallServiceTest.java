package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.OnCallShiftRequest;
import com.example.IncidentPulse.DTO.Response.OnCallShiftDTO;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapperImpl;
import com.example.IncidentPulse.Model.OnCallShift;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.OnCallShiftRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for OnCallService.
 *
 * Note OnCallService depends on the concrete UserMapperImpl (not the interface),
 * so that is what we @Mock here. Mockito can mock concrete classes just fine.
 */
@ExtendWith(MockitoExtension.class)
class OnCallServiceTest {

    @Mock
    private OnCallShiftRepository onCallShiftRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapperImpl userMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OnCallService onCallService;

    private User engineer() {
        User user = new User();
        user.setId(7L);
        user.setUsername("eng");
        user.setRole(User.Role.ENGINEER);
        return user;
    }

    // ---------------------------------------------------------------------
    // getUserOnCallNow
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getUserOnCallNow: returns the current on-call engineer")
    void getUserOnCallNow_returnsCurrentShift() {
        OnCallShift shift = new OnCallShift();
        shift.setUser_id(engineer());
        shift.setStartedAt(LocalDateTime.now().minusHours(1));
        shift.setEndAt(LocalDateTime.now().plusHours(1));

        when(onCallShiftRepository.findCurrentOnCallShift(any(LocalDateTime.class)))
                .thenReturn(Optional.of(shift));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().username("eng").build());

        OnCallShiftDTO result = onCallService.getUserOnCallNow();

        assertThat(result).isNotNull();
        assertThat(result.getUser().getUsername()).isEqualTo("eng");
        assertThat(result.getStartedAt()).isEqualTo(shift.getStartedAt());
        assertThat(result.getEndAt()).isEqualTo(shift.getEndAt());
    }

    @Test
    @DisplayName("getUserOnCallNow: throws NO_ON_CALL_ENGINEER when nobody is scheduled")
    void getUserOnCallNow_throwsWhenNoShift() {
        when(onCallShiftRepository.findCurrentOnCallShift(any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> onCallService.getUserOnCallNow())
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NO_ON_CALL_ENGINEER);
    }

    // ---------------------------------------------------------------------
    // createOnCallShift (admin creates for a specific user)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("createOnCallShift: persists a shift for the given user id")
    void createOnCallShift_savesForUser() {
        OnCallShiftRequest request = OnCallShiftRequest.builder()
                .startedAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(8))
                .build();

        when(userRepository.findById(7L)).thenReturn(Optional.of(engineer()));
        when(onCallShiftRepository.save(any(OnCallShift.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().username("eng").build());

        OnCallShiftDTO result = onCallService.createOnCallShift(7L, request);

        assertThat(result.getUser().getUsername()).isEqualTo("eng");

        // Verify the shift we persisted points at the right user and times.
        ArgumentCaptor<OnCallShift> captor = ArgumentCaptor.forClass(OnCallShift.class);
        verify(onCallShiftRepository).save(captor.capture());
        OnCallShift saved = captor.getValue();
        assertThat(saved.getUser_id().getId()).isEqualTo(7L);
        assertThat(saved.getStartedAt()).isEqualTo(request.getStartedAt());
        assertThat(saved.getEndAt()).isEqualTo(request.getEndAt());
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(emailService).sendOnCallShiftEmail(any(User.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("createOnCallShift: throws USER_NOT_FOUND for unknown user id")
    void createOnCallShift_throwsWhenUserMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                onCallService.createOnCallShift(404L, OnCallShiftRequest.builder().build()))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(onCallShiftRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // createOnCallShiftForCurrentUser (engineer creates for themselves)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("createOnCallShiftForCurrentUser: persists a shift for the authenticated user")
    void createOnCallShiftForCurrentUser_savesForSelf() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        OnCallShiftRequest request = OnCallShiftRequest.builder()
                .startedAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(8))
                .build();

        when(authentication.getName()).thenReturn("eng");
        when(userRepository.findUserByUsername("eng")).thenReturn(Optional.of(engineer()));
        when(onCallShiftRepository.save(any(OnCallShift.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(UserResponse.builder().username("eng").build());

        OnCallShiftDTO result = onCallService.createOnCallShiftForCurrentUser(request, authentication);

        assertThat(result.getUser().getUsername()).isEqualTo("eng");
        verify(onCallShiftRepository).save(any(OnCallShift.class));
        verify(emailService).sendOnCallShiftEmail(any(User.class), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("createOnCallShiftForCurrentUser: throws USER_NOT_FOUND when principal has no record")
    void createOnCallShiftForCurrentUser_throwsWhenMissing() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                onCallService.createOnCallShiftForCurrentUser(
                        OnCallShiftRequest.builder().build(), authentication))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(onCallShiftRepository, never()).save(any());
    }
}
