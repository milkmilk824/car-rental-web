package com.example.carrental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalInterfacesIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void adminCanBindStoreStaffAndStaffCanOnlyOperateBoundStores() throws Exception {
        String adminToken = login("admin", "123456");
        long staffUserId = createStoreStaff(adminToken, "qa_store_staff");
        String staffToken = login("qa_store_staff", "123456");
        JsonNode stores = getData(get("/api/stores"));
        long boundStoreId = stores.get(0).get("id").asLong();
        long unboundStoreId = stores.get(1).get("id").asLong();

        mockMvc.perform(post("/api/admin/stores/{storeId}/staff/{userId}", boundStoreId, staffUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.store.id").value(boundStoreId))
                .andExpect(jsonPath("$.data.user.id").value(staffUserId));

        mockMvc.perform(get("/api/store/my-stores")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(boundStoreId));

        mockMvc.perform(get("/api/store/orders")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("storeId", String.valueOf(boundStoreId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/store/orders")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("storeId", String.valueOf(unboundStoreId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前门店不属于该员工"));
    }

    @Test
    void carAvailabilityReflectsRequestedRentalWindow() throws Exception {
        String userToken = login("zhangsan", "123456");
        JsonNode car = firstAvailableCar();
        long carId = car.get("id").asLong();
        long storeId = car.at("/store/id").asLong();
        LocalDateTime start = LocalDateTime.now().plusDays(15).withNano(0);
        LocalDateTime end = start.plusDays(2);

        mockMvc.perform(get("/api/cars/{id}/availability", carId)
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));

        createOrder(userToken, carId, storeId, start, end);

        mockMvc.perform(get("/api/cars/{id}/availability", carId)
                        .param("startTime", start.plusHours(2).toString())
                        .param("endTime", end.minusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void adminCanUploadCarImageAndReadRevenueTrend() throws Exception {
        String adminToken = login("admin", "123456");
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "car.png",
                "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}
        );

        mockMvc.perform(multipart("/api/admin/upload/car-image")
                        .file(image)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/car-images/")));

        mockMvc.perform(get("/api/admin/dashboard/revenue-trend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].date").exists())
                .andExpect(jsonPath("$.data[0].revenue").exists());
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

    private long createStoreStaff(String adminToken, String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "123456",
                                  "phone": "18812349999",
                                  "role": "STORE_STAFF",
                                  "status": "ACTIVE"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/id").asLong();
    }

    private JsonNode firstAvailableCar() throws Exception {
        JsonNode cars = getData(get("/api/cars").param("status", "AVAILABLE").param("size", "1"));
        JsonNode items = cars.get("items");
        assertThat(items).isNotEmpty();
        return items.get(0);
    }

    private void createOrder(String userToken, long carId, long storeId, LocalDateTime start, LocalDateTime end) throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carId": %d,
                                  "pickupStoreId": %d,
                                  "returnStoreId": %d,
                                  "startTime": "%s",
                                  "endTime": "%s"
                                }
                                """.formatted(carId, storeId, storeId, start, end)))
                .andExpect(status().isOk());
    }

    private JsonNode getData(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }
}
