package com.example.IncidentPulse.Controller;

import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Repository.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Incident lifecycle + listing tests. Authenticated with @WithMockUser (the
 * state-machine/authorization behaviour is what's under test, not JWT itself).
 * @Transactional rolls back the seeded data after each test for isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IncidentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IncidentRepository incidentRepository;

    private Incident seed(Incident.status status, Incident.severity severity) {
        Incident incident = new Incident();
        incident.setTitle("Seeded incident");
        incident.setMessage("seed");
        incident.setStatus(status);
        incident.setSeverity(severity);
        return incidentRepository.save(incident);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateStatus_validTransition_returns200() throws Exception {
        Incident incident = seed(Incident.status.OPENED, Incident.severity.SEV2);

        mockMvc.perform(patch("/api/v1/incident/{id}/status", incident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INVESTIGATING", "note", "On it"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVESTIGATING"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateStatus_illegalTransition_returns400() throws Exception {
        Incident incident = seed(Incident.status.OPENED, Incident.severity.SEV2);

        mockMvc.perform(patch("/api/v1/incident/{id}/status", incident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CLOSED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getHistory_afterStatusChange_returnsAuditEntry() throws Exception {
        Incident incident = seed(Incident.status.OPENED, Incident.severity.SEV3);

        mockMvc.perform(patch("/api/v1/incident/{id}/status", incident.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INVESTIGATING"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/incident/{id}/history", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].toStatus").value("INVESTIGATING"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void list_filtersByStatus() throws Exception {
        seed(Incident.status.OPENED, Incident.severity.SEV1);
        seed(Incident.status.OPENED, Incident.severity.SEV2);
        seed(Incident.status.INVESTIGATING, Incident.severity.SEV1);

        mockMvc.perform(get("/api/v1/incident").param("status", "OPENED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].status", everyItem(org.hamcrest.Matchers.is("OPENED"))));
    }
}
