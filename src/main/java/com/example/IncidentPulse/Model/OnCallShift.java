package com.example.IncidentPulse.Model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Table(name= "on_call_shift")
public class OnCallShift {

    @Id
    Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="actor_user_id")
    User user_id;

    LocalDateTime startedAt;

    LocalDateTime endAt;

    LocalDateTime createdAt;
}
