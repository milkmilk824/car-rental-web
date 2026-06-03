package com.example.carrental;

import com.example.carrental.controller.TestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestControllerIntegrationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class DevProfile {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

        @Test
        void pingIsPublicAndShowsProfileGuard() throws Exception {
            mockMvc.perform(get("/api/test/ping"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("UP"))
                    .andExpect(jsonPath("$.data.activeProfiles", hasItem("test")))
                    .andExpect(jsonPath("$.data.controllerProfile").value("dev,test"));
        }

        @Test
        void databaseDiagnosticsRequireAdminAndReturnRepositoryCounts() throws Exception {
            mockMvc.perform(get("/api/test/database"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("请先登录"));

            String userToken = login("zhangsan", "123456");
            mockMvc.perform(get("/api/test/database")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("当前账号无权访问该接口"));

            String adminToken = login("admin", "123456");
            mockMvc.perform(get("/api/test/database")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.databaseConnected").value(true))
                    .andExpect(jsonPath("$.data.counts.users", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.data.counts.cars", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.data.counts.stores", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.data.checks[0].name").value("users"))
                    .andExpect(jsonPath("$.data.checks[0].status").value("PASS"));
        }

        @Test
        void authProbeShowsCurrentUserAndAdminOnlyEndpointChecksRole() throws Exception {
            String userToken = login("zhangsan", "123456");
            mockMvc.perform(get("/api/test/auth/me")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("zhangsan"))
                    .andExpect(jsonPath("$.data.role").value("USER"));

            mockMvc.perform(get("/api/test/auth/admin-only")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());

            String adminToken = login("admin", "123456");
            mockMvc.perform(get("/api/test/auth/admin-only")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.allowed").value(true))
                    .andExpect(jsonPath("$.data.requiredRole").value("ADMIN"));
        }

        private String login(String username, String password) throws Exception {
            MvcResult result = mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "%s", "password": "%s"}
                                    """.formatted(username, password)))
                    .andExpect(status().isOk())
                    .andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/token").asText();
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "spring.profiles.active=prod",
            "spring.datasource.url=jdbc:h2:mem:test_controller_prod_guard;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.open-in-view=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "app.data-init.enabled=false",
            "app.payment.callback-secret=test-prod-secret"
    })
    @AutoConfigureMockMvc
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ProdProfile {

        @Autowired
        ApplicationContext applicationContext;

        @Test
        void testControllerIsNotRegisteredInProdProfile() throws Exception {
            assertThat(applicationContext.getBeanNamesForType(TestController.class)).isEmpty();
        }
    }
}
