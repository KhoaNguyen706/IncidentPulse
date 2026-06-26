package com.example.IncidentPulse.DTO.Request;

import com.example.IncidentPulse.Model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRequest {

    @NotBlank
    String name;

    @NotBlank
    @Email
    String email;

    @NotBlank
    @Size(min = 3, max = 50)
    String username;

    @NotBlank
    @Size(min = 8, max = 100)
    String password;

    LocalDateTime createdAt;

    boolean active;

    @NotNull
    User.Role role;

    @NotNull
    User.Team team;
}
