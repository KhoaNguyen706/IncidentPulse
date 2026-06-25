package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.UpdatedUserRequest;
import com.example.IncidentPulse.DTO.Request.UserRequest;
import com.example.IncidentPulse.DTO.Response.UpdatedUserResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapper;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for UserService.
 *
 * UserService talks to a database (UserRepository) and a UserMapper. In a unit
 * test we do NOT use a real database - instead we hand the service FAKE versions
 * of those dependencies (mocks) and tell them how to behave. That keeps the test
 * fast, isolated, and focused only on the service's own logic.
 *
 * - @Mock        creates a fake collaborator.
 * - @InjectMocks creates a real UserService and injects the mocks into it.
 * - when(...).thenReturn(...) stubs what a mock returns.
 * - verify(...) checks that a mock was (or wasn't) called.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Jane");
        user.setUsername("jane");
        user.setEmail("jane@example.com");
        user.setRole(User.Role.ENGINEER);
        user.setTeam(User.Team.BACKEND);
        return user;
    }

    // ---------------------------------------------------------------------
    // addAUser
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("addAUser: saves a new user and returns the mapped response")
    void addAUser_savesNewUser() {
        UserRequest request = UserRequest.builder()
                .name("Jane")
                .username("jane")
                .email("jane@example.com")
                .password("secret123")
                .role(User.Role.ENGINEER)
                .team(User.Team.BACKEND)
                .build();

        User mappedEntity = sampleUser();
        User savedEntity = sampleUser();
        UserResponse expectedResponse = UserResponse.builder().username("jane").build();

        // Tell the mocks how to behave for this scenario.
        when(userMapper.toEntity(request)).thenReturn(mappedEntity);
        when(userRepository.findUserByUsername("jane")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedEntity);
        when(userMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        UserResponse result = userService.addAUser(request);

        assertThat(result).isSameAs(expectedResponse);

        // Capture the entity that was actually saved so we can assert the
        // password was hashed (not stored in plain text).
        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        String storedPassword = savedCaptor.getValue().getHashedPassword();
        assertThat(storedPassword).isEqualTo("$2a$10$hashedPassword");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    @DisplayName("addAUser: throws USER_EXISTED when username already taken")
    void addAUser_throwsWhenUserExists() {
        UserRequest request = UserRequest.builder().username("jane").build();
        when(userMapper.toEntity(request)).thenReturn(sampleUser());
        when(userRepository.findUserByUsername("jane")).thenReturn(Optional.of(sampleUser()));

        assertThatThrownBy(() -> userService.addAUser(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_EXISTED);

        // Important: when the user already exists, we must NOT save anything.
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // getAllUsers
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getAllUsers: returns the mapped list from the repository")
    void getAllUsers_returnsList() {
        List<User> users = List.of(sampleUser());
        List<UserResponse> mapped = List.of(UserResponse.builder().username("jane").build());

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponseList(users)).thenReturn(mapped);

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).isSameAs(mapped);
        verify(userRepository).findAll();
    }

    // ---------------------------------------------------------------------
    // deleteById
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("deleteById: deletes when the user exists")
    void deleteById_deletesWhenExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteById: throws USER_NON_EXISTED and does not delete when missing")
    void deleteById_throwsWhenMissing() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteById(99L))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NON_EXISTED);

        verify(userRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------------------
    // updateUser
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("updateUser: updates only the non-null fields provided")
    void updateUser_updatesProvidedFields() {
        User existing = sampleUser();
        UpdatedUserRequest request = UpdatedUserRequest.builder()
                .name("New Name")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        UpdatedUserResponse expected = UpdatedUserResponse.builder().name("New Name").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toUpdatedResponse(existing)).thenReturn(expected);

        UpdatedUserResponse result = userService.updateUser(1L, request);

        assertThat(result).isSameAs(expected);
        // The name and role should have been applied to the existing entity.
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getRole()).isEqualTo(User.Role.ADMIN);
        // Username was null in the request, so it must stay unchanged.
        assertThat(existing.getUsername()).isEqualTo("jane");
    }

    @Test
    @DisplayName("updateUser: throws USER_NON_EXISTED when id not found")
    void updateUser_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, UpdatedUserRequest.builder().build()))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NON_EXISTED);
    }

    // ---------------------------------------------------------------------
    // getMyinfo
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getMyinfo: returns the current authenticated user's info")
    void getMyinfo_returnsCurrentUser() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        UserResponse expected = UserResponse.builder().username("jane").build();

        when(authentication.getName()).thenReturn("jane");
        when(userRepository.findUserByUsername("jane")).thenReturn(Optional.of(sampleUser()));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        UserResponse result = userService.getMyinfo(authentication);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("getMyinfo: throws USER_NOT_FOUND when the username has no record")
    void getMyinfo_throwsWhenMissing() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("ghost");
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyinfo(authentication))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
