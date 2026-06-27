package com.example.IncidentPulse.Mapper;

import com.example.IncidentPulse.DTO.Request.UserRequest;
import com.example.IncidentPulse.DTO.Response.UpdatedUserResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRequest userRequest) {
        if (userRequest == null) {
            return null;
        }

        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setUsername(userRequest.getUsername());
        user.setRole(userRequest.getRole());
        user.setTeam(userRequest.getTeam());
        user.setActive(userRequest.isActive());
        // hashedPassword, id, and createdAt are not set (ignored)

        return user;
    }

    @Override
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .username(user.getUsername())
                .active(user.isActive())
                .role(user.getRole())
                .team(user.getTeam())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UpdatedUserResponse toUpdatedResponse(User user) {
        if (user == null) {
            return null;
        }

        return UpdatedUserResponse.builder()
                
                .name(user.getName())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .team(user.getTeam())
                .build();
    }

    @Override
    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) {
            return null;
        }

        List<UserResponse> list = new ArrayList<>(users.size());
        for (User user : users) {
            list.add(toResponse(user));
        }

        return list;
    }
}
