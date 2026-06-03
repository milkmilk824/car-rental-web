package com.example.carrental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionHardeningIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void loginRefreshAndLogoutManageTokenLifecycle() throws Exception {
        JsonNode login = login("zhangsan", "123456");
        String token = login.get("token").asText();
        String refreshToken = login.get("refreshToken").asText();
        assertThat(token).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        MvcResult refreshResult = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();
        JsonNode refreshed = objectMapper.readTree(refreshResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
        String refreshedToken = refreshed.get("token").asText();
        String rotatedRefreshToken = refreshed.get("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/user/logout")
                        .header("Authorization", "Bearer " + refreshedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocumentIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/user/login']").exists());
    }

    @Test
    void adminListsArePagedAndMutationsWriteAuditLogs() throws Exception {
        String adminToken = login("admin", "123456").get("token").asText();
        String username = "audit_user_" + System.nanoTime();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456",
                                  "phone": "18800009999",
                                  "role": "USER",
                                  "status": "ACTIVE"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2));

        String auditPayload = auditPayloadEventually(adminToken);
        assertThat(auditPayload).contains("\"path\":\"/api/admin/users\"");
        assertThat(auditPayload).contains("\"actorUsername\":\"admin\"");
        assertThat(auditPayload).contains("\"httpMethod\":\"POST\"");
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }

    private String auditPayloadEventually(String adminToken) throws Exception {
        String payload = "";
        for (int i = 0; i < 10; i++) {
            MvcResult auditResult = mockMvc.perform(get("/api/admin/audit-logs")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn();
            payload = auditResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            if (payload.contains("\"path\":\"/api/admin/users\"")) {
                return payload;
            }
            Thread.sleep(100);
        }
        return payload;
    }
}
