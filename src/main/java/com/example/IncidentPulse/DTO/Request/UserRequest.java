package com.example.IncidentPulse.DTO.Request;

import com.example.IncidentPulse.Model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    String name;
    String email;
    String username;
    String password;
    LocalDateTime createdAt;
    boolean active;
    User.Role role;
    User.Team team;
}
