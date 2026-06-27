package com.example.IncidentPulse.Controller;


import com.example.IncidentPulse.DTO.Request.IncidentRequest;
import com.example.IncidentPulse.DTO.Request.IncidentStatusUpdateRequest;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.DTO.Response.IncidentHistoryResponse;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.DTO.Response.PageResponse;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incident")
public class IncidentController {

    private final IncidentService incidentService;

    @Autowired
    public IncidentController(IncidentService incidentService){
        this.incidentService = incidentService;
    }

    @PostMapping
    public ApiResponse<IncidentResponse> createIncident(@Valid @RequestBody IncidentRequest incidentRequest){
        IncidentResponse incidentResponse = incidentService.createIncident(incidentRequest);
        return ApiResponse.<IncidentResponse>builder()
                .code(201)
                .now(LocalDateTime.now())
                .data(incidentResponse)
                .message("Create incident completely")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<IncidentResponse>> listIncidents(
            @RequestParam(required = false) Incident.status status,
            @RequestParam(required = false) Incident.severity severity,
            @RequestParam(required = false) String assignee,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<IncidentResponse> page = incidentService.findIncidents(status, severity, assignee, pageable);
        return ApiResponse.<PageResponse<IncidentResponse>>builder()
                .code(200)
                .success(true)
                .now(LocalDateTime.now())
                .data(page)
                .message("Incidents retrieved successfully!!!")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<List<IncidentResponse>> getMyIncidents(Authentication authentication){
        List<IncidentResponse> incidents = incidentService.getMyIncidents(authentication);
        return ApiResponse.<List<IncidentResponse>>builder()
                .code(200)
                .success(true)
                .now(LocalDateTime.now())
                .data(incidents)
                .message("Assigned incidents retrieved successfully")
                .build();
    }

    @PatchMapping("/{incidentId}")
    public ApiResponse<IncidentResponse> updateIncident(@PathVariable Long incidentId,
                                                        @Valid @RequestBody IncidentRequest incidentRequest){
        IncidentResponse incidentResponse = incidentService.updateIncident(incidentId, incidentRequest);
        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .now(LocalDateTime.now())
                .data(incidentResponse)
                .message("Update incident successfully!!!")
                .build();
    }

    @PreAuthorize("hasAnyRole('ENGINEER','ADMIN')")
    @PatchMapping("/{id}/status")
    public ApiResponse<IncidentResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody IncidentStatusUpdateRequest request){
        IncidentResponse incidentResponse = incidentService.updateStatus(id, request);
        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .success(true)
                .now(LocalDateTime.now())
                .data(incidentResponse)
                .message("Incident status updated successfully!!!")
                .build();
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<IncidentHistoryResponse>> getIncidentHistory(@PathVariable Long id){
        List<IncidentHistoryResponse> history = incidentService.getIncidentHistory(id);
        return ApiResponse.<List<IncidentHistoryResponse>>builder()
                .code(200)
                .success(true)
                .now(LocalDateTime.now())
                .data(history)
                .message("Incident history retrieved successfully!!!")
                .build();
    }
}
