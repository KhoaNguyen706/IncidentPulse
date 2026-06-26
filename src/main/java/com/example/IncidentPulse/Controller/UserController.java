package com.example.IncidentPulse.Controller;


import com.example.IncidentPulse.DTO.Request.UpdatedUserRequest;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.DTO.Request.UserRequest;
import com.example.IncidentPulse.DTO.Response.UpdatedUserResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    public final UserService userService;

    @Autowired
    public UserController(UserService userService){
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUser(){
        List<UserResponse> userResponse = userService.getAllUsers();

        return ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .data(userResponse)
                .success(true)
                .now(LocalDateTime.now())
                .message("Get all users successfully!")
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<UserResponse> createAUser(@Valid @RequestBody UserRequest userRequest){
            UserResponse userResponse = userService.addAUser(userRequest);
            return ApiResponse.<UserResponse>builder()
                    .code(201)
                    .data(userResponse)
                    .success(true)
                    .message("Created an user successfully!")
                    .now(LocalDateTime.now())
                    .build();

    }
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ApiResponse<UpdatedUserResponse> updateAUser(@PathVariable Long id,
                                                        @Valid @RequestBody UpdatedUserRequest updatedUserRequest){
        UpdatedUserResponse userResponse = userService.updateUser(id,updatedUserRequest);
        return ApiResponse.<UpdatedUserResponse>builder()
                .code(200)
                .data(userResponse)
                .success(true)
                .now(LocalDateTime.now())
                .message("Updated user successfully!!!")
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAUser(@PathVariable Long id){
        userService.deleteById(id);
        return ApiResponse.<Void>builder()
                .code(204)
                .success(true)
                .data(null)
                .now(LocalDateTime.now())
                .message("Delete a user successfully!!!")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(Authentication authentication){
        UserResponse userResponse = userService.getMyinfo(authentication);
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .success(true)
                .data(userResponse)
                .now(LocalDateTime.now())
                .message("Get your information completely!!!")
                .build();
    }
}
