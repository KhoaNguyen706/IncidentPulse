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
@Builder
@Table(name = "incident_history")
public class IncidentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    User actor;

    String message;

    @Enumerated(EnumType.STRING)
    ActionType actionType;

    @Enumerated(EnumType.STRING)
    Incident.status fromStatus;

    @Enumerated(EnumType.STRING)
    Incident.status toStatus;

    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ActionType {
        CREATED, ASSIGNED, ACKNOWLEDGED, STATUS_CHANGED, COMMENTED, RESOLVED, CLOSED
    }
}
