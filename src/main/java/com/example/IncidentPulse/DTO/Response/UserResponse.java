package com.example.IncidentPulse.DTO.Response;


import com.example.IncidentPulse.Model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    String name;
    String username;
    String email;
    boolean active;
    User.Role role;
    User.Team team;
    LocalDateTime createdAt;
}
