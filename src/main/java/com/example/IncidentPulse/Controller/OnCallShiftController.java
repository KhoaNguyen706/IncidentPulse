package com.example.IncidentPulse.Controller;

import com.example.IncidentPulse.DTO.Request.OnCallShiftRequest;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.DTO.Response.OnCallShiftDTO;
import com.example.IncidentPulse.Service.OnCallService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController()
@RequestMapping("/api/v1/on-call")
public class OnCallShiftController {

    private final OnCallService onCallService;

    public OnCallShiftController(OnCallService onCallService) {
        this.onCallService = onCallService;
    }

    /** Who is on call right now — visible to all authenticated staff. */
    @GetMapping
    public ApiResponse<OnCallShiftDTO> getCurrentOnCallEngineer() {
        OnCallShiftDTO current = onCallService.getCurrentOnCallOptional().orElse(null);
        return ApiResponse.<OnCallShiftDTO>builder()
                .success(true)
                .data(current)
                .message(current != null
                        ? "Current on-call engineer retrieved successfully"
                        : "No on-call engineer is currently scheduled")
                .build();
    }

    /** Full schedule for admins. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/shifts")
    public ApiResponse<List<OnCallShiftDTO>> listShifts() {
        List<OnCallShiftDTO> shifts = onCallService.listAllShifts();
        return ApiResponse.<List<OnCallShiftDTO>>builder()
                .success(true)
                .data(shifts)
                .message("On-call schedule retrieved successfully")
                .build();
    }

    /** Active on-call window for the logged-in engineer, if any. */
    @GetMapping("/me")
    public ApiResponse<OnCallShiftDTO> getMyOnCallShift(Authentication authentication) {
        OnCallShiftDTO shift = onCallService.getMyActiveShift(authentication).orElse(null);
        return ApiResponse.<OnCallShiftDTO>builder()
                .success(true)
                .now(LocalDateTime.now())
                .data(shift)
                .message(shift != null
                        ? "Your active on-call shift"
                        : "You are not currently on call")
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}")
    public ApiResponse<OnCallShiftDTO> createOnCallShift(@PathVariable Long userId,
                                                         @RequestBody OnCallShiftRequest request) {
        OnCallShiftDTO onCallShift = onCallService.createOnCallShift(userId, request);
        return ApiResponse.<OnCallShiftDTO>builder()
                .data(onCallShift)
                .message("On-call shift created successfully")
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/me")
    public ApiResponse<OnCallShiftDTO> createMyOnCallShift(Authentication authentication,
                                                           @RequestBody OnCallShiftRequest request) {
        OnCallShiftDTO onCallShift = onCallService.createOnCallShiftForCurrentUser(request, authentication);
        return ApiResponse.<OnCallShiftDTO>builder()
                .data(onCallShift)
                .message("Your on-call shift created successfully")
                .build();
    }
}
