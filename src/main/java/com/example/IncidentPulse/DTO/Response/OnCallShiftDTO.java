package com.example.IncidentPulse.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnCallShiftDTO implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    UserResponse user;
    LocalDateTime startedAt;
    LocalDateTime endAt;
}
