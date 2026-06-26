package com.example.IncidentPulse.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Table(name= "users")
public class User implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Email
    @Column(nullable = false,unique = true)
    String email;

    @Column(nullable = false)
    String username;

    @Column(nullable = false)
    String hashedPassword;

    LocalDateTime createdAt;

    boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Team team;




    public enum Team{PAYMENT,API,SERVICE,SYSTEM,FRONTEND,BACKEND,SECURITY,NETWORK}

    public enum Role{ADMIN,ENGINEER,VIEWER,MANAGER}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
