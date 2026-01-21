package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.OnCallShiftRequest;
import com.example.IncidentPulse.DTO.Response.OnCallShiftDTO;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Model.OnCallShift;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.OnCallShiftRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OnCallService {

    private final OnCallShiftRepository callShiftRepository;
    private final UserRepository userRepository;

    public OnCallService(OnCallShiftRepository onCallShiftRepository, UserRepository userRepository){
        this.callShiftRepository = onCallShiftRepository;
        this.userRepository = userRepository;
    }

    public OnCallShiftDTO getUserOnCallNow(){
        LocalDateTime now = LocalDateTime.now();
        OnCallShift currentShift = callShiftRepository.findCurrentOnCallShift(now)
                .orElseThrow(() -> new AppException(ErrorCode.NO_ON_CALL_ENGINEER));
        
        return OnCallShiftDTO.builder()
                .user(currentShift.getUser_id())
                .startedAt(currentShift.getStartedAt())
                .endAt(currentShift.getEndAt())
                .build();
    }

    public OnCallShiftDTO createOnCallShift(OnCallShiftRequest request){
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        OnCallShift onCallShift = new OnCallShift();
        onCallShift.setUser_id(user);
        onCallShift.setStartedAt(request.getStartedAt());
        onCallShift.setEndAt(request.getEndAt());
        onCallShift.setCreatedAt(LocalDateTime.now());
        
        OnCallShift savedShift = callShiftRepository.save(onCallShift);
        
        return OnCallShiftDTO.builder()
                .user(savedShift.getUser_id())
                .startedAt(savedShift.getStartedAt())
                .endAt(savedShift.getEndAt())
                .build();
    }
}
