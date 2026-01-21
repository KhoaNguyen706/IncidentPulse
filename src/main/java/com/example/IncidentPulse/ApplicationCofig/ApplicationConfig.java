package com.example.IncidentPulse.ApplicationCofig;

import ch.qos.logback.classic.encoder.JsonEncoder;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
    @Bean
    CommandLineRunner initAccount(UserRepository userRepository,PasswordEncoder passwordEncoder){
        return args -> {
            if(userRepository.findUserByUsername("admin").isEmpty()){
                User admin = new User();
                admin.setName("Admin User");
                admin.setEmail("admin@incidentpulse.com");
                admin.setUsername("admin");
                String rawPassword = "admin123";
                String hashedPassword = passwordEncoder().encode(rawPassword);
                admin.setHashedPassword(hashedPassword);
                admin.setRole(User.Role.ADMIN);
                admin.setTeam(User.Team.BACKEND);
                admin.setCreatedAt(LocalDateTime.now());
                userRepository.save(admin);

                log.info("Admin account have been created!!!");
            }else{

                log.warn("Admin account is already existed!!!");
            }
        };
    }


}
