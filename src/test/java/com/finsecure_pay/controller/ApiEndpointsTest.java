package com.finsecure_pay.controller;


import com.finsecure_pay.model.TransactionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
class ApiEndpointsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /health reports UP")
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("finsecure-pay"));
    }

    @Test
    @DisplayName("POST /validate returns 200 for a valid transaction")
    void validateAcceptsValidTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN-1001",
                new BigDecimal("250.75"),
                "SGD",
                "123456789012",
                "210987654321"
        );

        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("POST /validate returns 422 with reasons for an invalid transaction")
    void validateRejectsInvalidTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest(
                "TXN-1002",
                new BigDecimal("-5"),
                "ABC",
                "1234567890",
                "1234567890"
        );

        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors.length()").value(4));
    }


    @Test
    @DisplayName("POST /validate returns 400 for malformed JSON")
    void validateRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Prometheus metrics endpoint is exposed for monitoring")
    void prometheusEndpointExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

}
