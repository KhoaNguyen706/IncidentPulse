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
@Table(name= "incident")
public class Incident {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String title;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="created_by_user_id")
    User createdBy;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="assigned_to_user_id")
    User assignedTo;

    @Enumerated(EnumType.STRING)
    status status;

    @Enumerated(EnumType.STRING)
    severity severity;



    String message;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum severity{
        SEV4,
        SEV3,
        SEV2,
        SEV1
    }

    public enum status{
        OPENED,
        INVESTIGATING,
        RESOLVED,
        CLOSED,
    }
}


