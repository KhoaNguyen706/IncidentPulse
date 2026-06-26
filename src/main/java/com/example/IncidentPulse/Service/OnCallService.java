package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.OnCallShiftRequest;
import com.example.IncidentPulse.DTO.Response.OnCallShiftDTO;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;

import com.example.IncidentPulse.Mapper.UserMapperImpl;
import com.example.IncidentPulse.Model.OnCallShift;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.ApplicationCofig.CachingConfig;
import com.example.IncidentPulse.Repository.OnCallShiftRepository;
import com.example.IncidentPulse.Repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OnCallService {

    private final OnCallShiftRepository callShiftRepository;
    private final UserRepository userRepository;
    private final UserMapperImpl userMapper;

    public OnCallService(OnCallShiftRepository onCallShiftRepository, UserRepository userRepository, UserMapperImpl userMapper){
        this.callShiftRepository = onCallShiftRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CachingConfig.ON_CALL_CURRENT, key = "'current'")
    public OnCallShiftDTO getUserOnCallNow(){
        LocalDateTime now = LocalDateTime.now();
        OnCallShift currentShift = callShiftRepository.findCurrentOnCallShift(now)
                .orElseThrow(() -> new AppException(ErrorCode.NO_ON_CALL_ENGINEER));
        
        return OnCallShiftDTO.builder()
                .user(userMapper.toResponse(currentShift.getUser_id()))
                .startedAt(currentShift.getStartedAt())
                .endAt(currentShift.getEndAt())
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = CachingConfig.ON_CALL_CURRENT, allEntries = true)
    public OnCallShiftDTO createOnCallShift(Long userId, OnCallShiftRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        OnCallShift onCallShift = new OnCallShift();
        onCallShift.setUser_id(user);
        onCallShift.setStartedAt(request.getStartedAt());
        onCallShift.setEndAt(request.getEndAt());
        onCallShift.setCreatedAt(LocalDateTime.now());
        
        OnCallShift savedShift = callShiftRepository.save(onCallShift);
        
        return OnCallShiftDTO.builder()
                .user(userMapper.toResponse(savedShift.getUser_id()))
                .startedAt(savedShift.getStartedAt())
                .endAt(savedShift.getEndAt())
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = CachingConfig.ON_CALL_CURRENT, allEntries = true)
    public OnCallShiftDTO createOnCallShiftForCurrentUser(OnCallShiftRequest request, org.springframework.security.core.Authentication authentication){
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        OnCallShift onCallShift = new OnCallShift();
        onCallShift.setUser_id(user);
        onCallShift.setStartedAt(request.getStartedAt());
        onCallShift.setEndAt(request.getEndAt());
        onCallShift.setCreatedAt(LocalDateTime.now());

        OnCallShift savedShift = callShiftRepository.save(onCallShift);

        return OnCallShiftDTO.builder()
                .user(userMapper.toResponse(savedShift.getUser_id()))
                .startedAt(savedShift.getStartedAt())
                .endAt(savedShift.getEndAt())
                .build();
    }
}
