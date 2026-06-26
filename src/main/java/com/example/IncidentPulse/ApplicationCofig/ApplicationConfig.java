package com.example.IncidentPulse.ApplicationCofig;

import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Seeds a default admin account on first startup so the app is usable
     * out-of-the-box. Credentials come from environment variables; in
     * production you SHOULD set these and rotate the password immediately
     * after the first login.
     */
    @Bean
    CommandLineRunner initAccount(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.admin.username:admin}") String adminUsername,
                                  @Value("${app.admin.email:admin@incidentpulse.local}") String adminEmail,
                                  @Value("${app.admin.password:admin123}") String adminPassword) {
        return args -> {
            if (userRepository.findUserByUsername(adminUsername).isEmpty()) {
                User admin = new User();
                admin.setName("Admin User");
                admin.setEmail(adminEmail);
                admin.setUsername(adminUsername);
                admin.setHashedPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(User.Role.ADMIN);
                admin.setTeam(User.Team.BACKEND);
                admin.setActive(true);
                userRepository.save(admin);
                log.info("Admin account '{}' created. Change the password after first login.", adminUsername);
            } else {
                log.info("Admin account '{}' already exists - skipping seed.", adminUsername);
            }
        };
    }
}
