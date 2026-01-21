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
    @PostMapping
    public ApiResponse<OnCallShiftDTO> createOnCallShift(@RequestBody OnCallShiftRequest request){
        OnCallShiftDTO onCallShift = onCallService.createOnCallShift(request);
        return ApiResponse.<OnCallShiftDTO>builder()
                .data(onCallShift)
                .message("On-call shift created successfully")
                .build();
    }
}
