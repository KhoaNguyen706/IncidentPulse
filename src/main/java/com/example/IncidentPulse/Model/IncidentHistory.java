package com.example.IncidentPulse.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Table(name= "incident_history")
public class IncidentHistory {

    @Id
    Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="incident_id")
    Incident incident_id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="actor_user_id")
    User actor_user_id;

    String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    status from_status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    status to_status;


    public enum  status{
        OPENED,
        INVESTIGATING,
        RESOLVED,
        CLOSED,
    }

    public enum action_type {
        CREATED, ASSIGNED, ACKNOWLEDGED, STATUS_CHANGED, COMMENTED, RESOLVED, CLOSED,
    }
}
