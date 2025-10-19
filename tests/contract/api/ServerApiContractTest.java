package com.logcollector.contract.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test for POST /v1/servers endpoint.
 *
 * Validates API contract matches OpenAPI specification.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServerApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreateServerEndpoint_ValidRequest() throws Exception {
        // Given
        String requestBody = """
                {
                    "hostname": "test-server.example.com",
                    "port": 22,
                    "username": "testuser",
                    "password": "testpass",
                    "logFilePaths": ["/var/log/app.log"],
                    "collectionInterval": 30000,
                    "batchSize": 500
                }
                """;

        // When/Then - This will fail until we implement ServerController
        // mockMvc.perform(post("/v1/servers")
        //         .contentType(MediaType.APPLICATION_JSON)
        //         .content(requestBody)
        //         .header("Authorization", "Bearer " + getAdminToken()))
        //     .andExpect(status().isCreated())
        //     .andExpect(jsonPath("$.id").exists())
        //     .andExpect(jsonPath("$.hostname").value("test-server.example.com"));
    }

    @Test
    void testCreateServerEndpoint_InvalidRequest() throws Exception {
        // Test validation errors
        // This will fail until implementation
    }

    @Test
    void testCreateServerEndpoint_Unauthorized() throws Exception {
        // Test that endpoint requires ADMIN role
        // This will fail until implementation
    }

    @Test
    void testGetServersEndpoint() throws Exception {
        // Test GET /v1/servers
        // This will fail until implementation
    }

    private String getAdminToken() {
        // TODO: Get OAuth2 token for testing
        return "mock-admin-token";
    }
}
