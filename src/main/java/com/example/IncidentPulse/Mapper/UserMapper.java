package com.example.IncidentPulse.Mapper;

import com.example.IncidentPulse.DTO.Request.UserRequest;
import com.example.IncidentPulse.DTO.Response.UpdatedUserResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Model.User;

import java.util.List;

// Manual implementation provided in UserMapperImpl.java
public interface UserMapper {

    User toEntity(UserRequest userRequest);


    UserResponse toResponse(User user);
    UpdatedUserResponse toUpdatedResponse(User user);
    List<UserResponse> toResponseList(List<User> users);
}
