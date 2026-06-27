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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OnCallService {

    private static final Logger log = LoggerFactory.getLogger(OnCallService.class);

    private final OnCallShiftRepository callShiftRepository;
    private final UserRepository userRepository;
    private final UserMapperImpl userMapper;
    private final EmailService emailService;

    public OnCallService(OnCallShiftRepository onCallShiftRepository,
                         UserRepository userRepository,
                         UserMapperImpl userMapper,
                         EmailService emailService) {
        this.callShiftRepository = onCallShiftRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CachingConfig.ON_CALL_CURRENT, key = "'current'")
    public OnCallShiftDTO getUserOnCallNow() {
        LocalDateTime now = LocalDateTime.now();
        OnCallShift currentShift = callShiftRepository.findCurrentOnCallShift(now)
                .orElseThrow(() -> new AppException(ErrorCode.NO_ON_CALL_ENGINEER));

        return toDto(currentShift, now);
    }

    @Transactional(readOnly = true)
    public Optional<OnCallShiftDTO> getCurrentOnCallOptional() {
        LocalDateTime now = LocalDateTime.now();
        return callShiftRepository.findCurrentOnCallShift(now).map(shift -> toDto(shift, now));
    }

    @Transactional(readOnly = true)
    public List<OnCallShiftDTO> listAllShifts() {
        LocalDateTime now = LocalDateTime.now();
        return callShiftRepository.findAllWithUserOrderByStartedAtDesc().stream()
                .map(shift -> toDto(shift, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OnCallShiftDTO> getMyActiveShift(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        return callShiftRepository.findActiveShiftsForUser(user.getId(), now).stream()
                .findFirst()
                .map(shift -> toDto(shift, now));
    }

    @Transactional
    @CacheEvict(cacheNames = CachingConfig.ON_CALL_CURRENT, allEntries = true)
    public OnCallShiftDTO createOnCallShift(Long userId, OnCallShiftRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        OnCallShift savedShift = saveShift(user, request);
        notifyEngineer(user, savedShift.getStartedAt(), savedShift.getEndAt());
        return toDto(savedShift, LocalDateTime.now());
    }

    @Transactional
    @CacheEvict(cacheNames = CachingConfig.ON_CALL_CURRENT, allEntries = true)
    public OnCallShiftDTO createOnCallShiftForCurrentUser(OnCallShiftRequest request,
                                                          Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        OnCallShift savedShift = saveShift(user, request);
        notifyEngineer(user, savedShift.getStartedAt(), savedShift.getEndAt());
        return toDto(savedShift, LocalDateTime.now());
    }

    private OnCallShift saveShift(User user, OnCallShiftRequest request) {
        OnCallShift onCallShift = new OnCallShift();
        onCallShift.setUser_id(user);
        onCallShift.setStartedAt(request.getStartedAt());
        onCallShift.setEndAt(request.getEndAt());
        onCallShift.setCreatedAt(LocalDateTime.now());
        return callShiftRepository.save(onCallShift);
    }

    private void notifyEngineer(User user, LocalDateTime startedAt, LocalDateTime endAt) {
        try {
            emailService.sendOnCallShiftEmail(user, startedAt, endAt);
        } catch (Exception e) {
            log.warn("Failed to send on-call shift email: {}", e.getMessage());
        }
    }

    private OnCallShiftDTO toDto(OnCallShift shift, LocalDateTime now) {
        return OnCallShiftDTO.builder()
                .id(shift.getId())
                .user(userMapper.toResponse(shift.getUser_id()))
                .startedAt(shift.getStartedAt())
                .endAt(shift.getEndAt())
                .status(computeShiftStatus(shift, now))
                .build();
    }

    private String computeShiftStatus(OnCallShift shift, LocalDateTime now) {
        if (now.isBefore(shift.getStartedAt())) {
            return "UPCOMING";
        }
        if (now.isAfter(shift.getEndAt())) {
            return "ENDED";
        }
        return "ACTIVE";
    }
}
