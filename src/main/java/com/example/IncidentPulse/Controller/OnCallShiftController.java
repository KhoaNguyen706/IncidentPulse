package com.example.IncidentPulse.Controller;

import com.example.IncidentPulse.DTO.Request.OnCallShiftRequest;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.DTO.Response.OnCallShiftDTO;
import com.example.IncidentPulse.Service.OnCallService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/v1/on-call")
public class OnCallShiftController {

    private final OnCallService onCallService;

    public OnCallShiftController(OnCallService onCallService) {
        this.onCallService = onCallService;
    }

    @PreAuthorize("hasRole(ADMIN)")
    @GetMapping
    public ApiResponse<OnCallShiftDTO> getCurrentOnCallEngineer(){
        OnCallShiftDTO currentOnCall = onCallService.getUserOnCallNow();
        return ApiResponse.<OnCallShiftDTO>builder()
                .data(currentOnCall)
                .message("Current on-call engineer retrieved successfully")
                .build();
    }

    @PreAuthorize("hasRole(ADMIN)")
    @PostMapping("/{userId}")
    public ApiResponse<OnCallShiftDTO> createOnCallShift(@PathVariable Long userId, @RequestBody OnCallShiftRequest request){
        OnCallShiftDTO onCallShift = onCallService.createOnCallShift(userId, request);
        return ApiResponse.<OnCallShiftDTO>builder()
                .data(onCallShift)
                .message("On-call shift created successfully")
                .build();
    }

    @PreAuthorize("hasAnyRole('ENGINEER','ADMIN')")
    @PostMapping("/me")
    public ApiResponse<OnCallShiftDTO> createMyOnCallShift(org.springframework.security.core.Authentication authentication, @RequestBody OnCallShiftRequest request){
        OnCallShiftDTO onCallShift = onCallService.createOnCallShiftForCurrentUser(request, authentication);
        return ApiResponse.<OnCallShiftDTO>builder()
                .data(onCallShift)
                .message("Your on-call shift created successfully")
                .build();
    }
}
