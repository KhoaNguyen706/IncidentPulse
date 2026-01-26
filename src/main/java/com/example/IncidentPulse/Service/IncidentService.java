package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.IncidentRequest;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapper;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.OnCallShift;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.IncidentRepository;
import com.example.IncidentPulse.Repository.OnCallShiftRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.LocalDateTime;


import static com.example.IncidentPulse.Security.SecurityUtil.getCurrentUser;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OnCallShiftRepository onCallShiftRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    @Autowired
    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           UserMapper userMapper,
                           OnCallShiftRepository onCallShiftRepository,
                           EmailService emailService){
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.onCallShiftRepository = onCallShiftRepository;
        this.emailService = emailService;
    }

    public User assignUser(){
        // Find the current on-call shift and return the associated user
        OnCallShift onCallShift = onCallShiftRepository.findCurrentOnCallShift(LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.NO_ON_CALL_ENGINEER));

        User assignedUser = onCallShift.getUser_id();
        if (assignedUser == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        // Ensure fresh user instance from repository if needed
        return userRepository.findById(assignedUser.getId()).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));
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


        try{
            emailService.sendAssignmentEmail(assignUser(), incident);
        } catch (Exception e){
            log.warn("Failed to send assignment email: {}", e.getMessage());
        }
        
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
