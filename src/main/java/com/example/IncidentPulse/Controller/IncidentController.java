package com.example.IncidentPulse.Controller;


import com.example.IncidentPulse.DTO.Request.IncidentRequest;
import com.example.IncidentPulse.DTO.Response.ApiResponse;
import com.example.IncidentPulse.DTO.Response.IncidentResponse;
import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Service.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/incident")
public class IncidentController {

    private final IncidentService incidentService;

    @Autowired
    public IncidentController(IncidentService incidentService){
        this.incidentService = incidentService;
    }

    @PostMapping
    public ApiResponse<IncidentResponse> createIncident (@RequestBody IncidentRequest incidentRequest){
        IncidentResponse incidentResponse = incidentService.createIncident(incidentRequest);
        return ApiResponse.<IncidentResponse>builder()
                .code(201)
                .now(LocalDateTime.now())
                .data(incidentResponse)
                .message("Create incident completely")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<IncidentResponse> getMyIncident(Authentication authentication){
        IncidentResponse incidentResponse = incidentService.getMyIncident(authentication);
        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .now(LocalDateTime.now())
                .data(incidentResponse)
                .message("get an incident successfully!!!")
                .build();
    }
}
