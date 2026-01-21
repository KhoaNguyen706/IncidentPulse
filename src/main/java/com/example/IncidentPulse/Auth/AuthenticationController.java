package com.example.IncidentPulse.Auth;


import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


@RestController
@RequestMapping("api/v1/auth")
public class AuthenticationController {

    private  final AuthenticationService authenticationService;
    private final UserService userService;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService,UserService userService){
            this.authenticationService = authenticationService;
            this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> logIn(@RequestBody AuthenticationRequest authenticationRequest){
        AuthenticationResponse authenticationResponse = authenticationService.logIn(authenticationRequest);
        return ApiResponse.<AuthenticationResponse>builder()
                .code(200)
                .data(authenticationResponse)
                .success(true)
                .now(LocalDateTime.now())
                .message("Log in successfully!!!")
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest refreshRequest){
        AuthenticationResponse authenticationResponse = authenticationService.refresh(refreshRequest.getRefreshToken());

        return ApiResponse.<AuthenticationResponse>
                builder()
                .code(200)
                .data(authenticationResponse)
                .now(LocalDateTime.now())
                .success(true)
                .message("Refresh token successfully!!!")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logOut(){
        return ApiResponse.<Void>builder()
                .code(200)
                .data(null)
                .now(LocalDateTime.now())
                .success(true)
                .message("Log out successfully!!!")
                .build();
    }

}
