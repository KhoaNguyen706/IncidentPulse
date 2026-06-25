package com.example.IncidentPulse.Mapper;

import com.example.IncidentPulse.DTO.Request.UserRequest;
import com.example.IncidentPulse.DTO.Response.UpdatedUserResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test: the mapper has no dependencies, so we need no mocks at all.
 * This is the simplest kind of test - just create input, call the method,
 * assert on the output. Start here to understand the Arrange-Act-Assert pattern.
 */
class UserMapperImplTest {

    private UserMapperImpl mapper;

    @BeforeEach
    void setUp() {
        // Runs before EACH @Test so every test gets a clean, independent instance.
        mapper = new UserMapperImpl();
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("maps all fields from request to entity")
        void mapsFieldsFromRequest() {
            // Arrange: build the input
            UserRequest request = UserRequest.builder()
                    .name("Jane Doe")
                    .email("jane@example.com")
                    .username("jane")
                    .password("secret123")
                    .role(User.Role.ENGINEER)
                    .team(User.Team.BACKEND)
                    .build();

            // Act: call the method under test
            User result = mapper.toEntity(request);

            // Assert: verify the output
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Jane Doe");
            assertThat(result.getEmail()).isEqualTo("jane@example.com");
            assertThat(result.getUsername()).isEqualTo("jane");
            assertThat(result.getRole()).isEqualTo(User.Role.ENGINEER);
            assertThat(result.getTeam()).isEqualTo(User.Team.BACKEND);
            // The mapper intentionally does NOT copy the password into the entity.
            assertThat(result.getHashedPassword()).isNull();
        }

        @Test
        @DisplayName("returns null when request is null")
        void returnsNullForNullInput() {
            assertThat(mapper.toEntity(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("maps user entity to response DTO")
        void mapsUserToResponse() {
            User user = new User();
            user.setName("John");
            user.setUsername("john");
            user.setEmail("john@example.com");
            user.setRole(User.Role.ADMIN);
            user.setTeam(User.Team.SECURITY);

            UserResponse result = mapper.toResponse(user);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("John");
            assertThat(result.getUsername()).isEqualTo("john");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
            assertThat(result.getTeam()).isEqualTo(User.Team.SECURITY);
        }

        @Test
        @DisplayName("returns null when user is null")
        void returnsNullForNullInput() {
            assertThat(mapper.toResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toUpdatedResponse()")
    class ToUpdatedResponse {

        @Test
        @DisplayName("maps user entity to updated response DTO")
        void mapsUserToUpdatedResponse() {
            User user = new User();
            user.setName("Updated Name");
            user.setUsername("updated");
            user.setEmail("updated@example.com");
            user.setRole(User.Role.MANAGER);
            user.setTeam(User.Team.PAYMENT);

            UpdatedUserResponse result = mapper.toUpdatedResponse(user);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getRole()).isEqualTo(User.Role.MANAGER);
            assertThat(result.getTeam()).isEqualTo(User.Team.PAYMENT);
        }
    }

    @Nested
    @DisplayName("toResponseList()")
    class ToResponseList {

        @Test
        @DisplayName("maps every user in the list")
        void mapsAllUsers() {
            User first = new User();
            first.setUsername("first");
            User second = new User();
            second.setUsername("second");

            List<UserResponse> result = mapper.toResponseList(List.of(first, second));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserResponse::getUsername)
                    .containsExactly("first", "second");
        }

        @Test
        @DisplayName("returns null when list is null")
        void returnsNullForNullInput() {
            assertThat(mapper.toResponseList(null)).isNull();
        }
    }
}
