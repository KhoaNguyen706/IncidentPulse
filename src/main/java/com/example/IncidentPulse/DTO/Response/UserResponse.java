package com.example.IncidentPulse.DTO.Response;


import com.example.IncidentPulse.Model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.PrePersist;
import jdk.jshell.Snippet;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    String name;
    String username;
    String email;
    boolean active;
    User.Role role;
    User.Team team;
    LocalDateTime createdAt;



    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
