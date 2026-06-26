package com.example.IncidentPulse.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Webhook ingestion tests: API-key auth and alert deduplication.
 * The test profile configures app.webhook.api-key=test-webhook-key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebhookControllerIT {

    private static final String VALID_KEY = "test-webhook-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String alertPayload(String externalId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("source", "uptimerobot");
        body.put("externalId", externalId);
        body.put("title", "API gateway down");
        body.put("severity", "SEV1");
        body.put("message", "HTTP 500 for 3 consecutive checks");
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void wrongApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/webhook/alert")
                        .header("X-API-Key", "not-the-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("monitor-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validPayload_createsIncident() throws Exception {
        mockMvc.perform(post("/api/v1/webhook/alert")
                        .header("X-API-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("monitor-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("API gateway down"))
                .andExpect(jsonPath("$.data.status").value("OPENED"));
    }

    @Test
    void duplicateExternalId_returnsSameIncident() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/webhook/alert")
                        .header("X-API-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("monitor-3")))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/webhook/alert")
                        .header("X-API-Key", VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("monitor-3")))
                .andExpect(status().isOk())
                .andReturn();

        long firstId = idOf(first);
        long secondId = idOf(second);
        assertThat(secondId).isEqualTo(firstId);
    }

    private long idOf(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}
