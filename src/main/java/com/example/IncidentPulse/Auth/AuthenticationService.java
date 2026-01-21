package com.example.IncidentPulse.Auth;

import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.UserRepository;
import com.example.IncidentPulse.Security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private  final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder encoder,
            JwtService jwtService
    ){
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }



    public AuthenticationResponse logIn(AuthenticationRequest authenticationRequest){
        User user = userRepository.findUserByUsername(authenticationRequest.getUsername())
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));

        if(!encoder.matches(authenticationRequest.getPassword(), user.getHashedPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // Convert User to UserDetails for JWT generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getHashedPassword())
                .authorities(user.getRole().toString())
                .build();
        
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("Tokens generated - Access: {}, Refresh: {}",
                accessToken != null ? "YES" : "NO",
                refreshToken != null ? "YES" : "NO");

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .access(accessToken)
                .refresh(refreshToken)
                .success(true)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Response created: {}", authenticationResponse);

        return authenticationResponse;
    }

    public AuthenticationResponse refresh(String refreshToken){
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Convert User to UserDetails for JWT generation
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getHashedPassword())
                .authorities("ROLE_" + user.getRole().toString())
                .build();
        
        String newAccess = jwtService.generateAccessToken(userDetails);
        
        return AuthenticationResponse.builder()
                .access(newAccess)
                .refresh(refreshToken)
                .build();
    }


}
