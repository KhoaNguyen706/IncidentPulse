package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.IncidentRequest;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapper;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.IncidentRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Random;

import static com.example.IncidentPulse.Security.SecurityUtil.getCurrentUser;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository, UserMapper userMapper){
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public User assignUser(){
        // Use database-level random selection to avoid loading all users
        return userRepository.findRandomUser()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }


    public IncidentResponse createIncident(IncidentRequest incidentRequest){
        User currentUser = getCurrentUser();
        Incident incident = new Incident();
        incident.setTitle(incidentRequest.getTitle());
        incident.setCreatedBy(currentUser);

        incident.setMessage(incidentRequest.getMessage());

        incident.setAssignedTo(assignUser());
        incident.setSeverity(incidentRequest.getSeverity());
        incident.setStatus(Incident.status.OPENED);
        incidentRepository.save(incident);
        
        // Convert User entities to UserResponse DTOs to hide sensitive data
        return IncidentResponse.builder()
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .title(incident.getTitle())
                .message(incident.getMessage())
                .assignedTo(userMapper.toResponse(incident.getAssignedTo()))
                .createdBy(userMapper.toResponse(incident.getCreatedBy()))
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    public IncidentResponse getMyIncident(Authentication authentication){
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));
        Incident incident = incidentRepository.findIncidentByAssignedTo(user);
        return IncidentResponse.builder()
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .title(incident.getTitle())
                .message(incident.getMessage())
                .createdBy(userMapper.toResponse(incident.getCreatedBy()))
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

}
