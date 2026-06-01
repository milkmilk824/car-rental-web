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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fullRentalFlow() throws Exception {
        String userToken = login("zhangsan", "123456");
        String staffToken = login("staff", "123456");

        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "13800008888", "email": "zhangsan@example.com", "realName": "张三"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("张三"));

        mockMvc.perform(post("/api/user/license")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName": "张三", "idCard": "310101199001018888", "driverLicenseNo": "SH888888"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driverLicenseNo").value("SH8****8888"));

        MvcResult carsResult = mockMvc.perform(get("/api/cars")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").exists())
                .andReturn();
        JsonNode firstCar = objectMapper.readTree(carsResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/items/0");
        long carId = firstCar.get("id").asLong();
        long storeId = firstCar.at("/store/id").asLong();

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime end = start.plusDays(3);
        String orderBody = """
                {
                  "carId": %d,
                  "pickupStoreId": %d,
                  "returnStoreId": %d,
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(carId, storeId, storeId, start, end);

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn();
        long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/id").asLong();

        MvcResult paymentResult = mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d, "payType": "MOCK"}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("WAITING"))
                .andReturn();
        String paymentNo = objectMapper.readTree(paymentResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/paymentNo").asText();

        mockMvc.perform(post("/api/payments/{paymentNo}/simulate-success", paymentNo)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/contracts/order/{orderId}", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractNo").exists());

        mockMvc.perform(put("/api/store/orders/{orderId}/pickup", orderId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RENTING"));

        mockMvc.perform(put("/api/orders/{orderId}/renew", orderId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"extraDays": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rentalDays").value(5));

        mockMvc.perform(put("/api/store/orders/{orderId}/return", orderId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d, "score": 5, "content": "车况很好，取还车流程顺畅。"}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(5));

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d, "score": 4, "content": "重复评价应被拒绝。"}
                                """.formatted(orderId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该订单已评价"));

        MvcResult commentsResult = mockMvc.perform(get("/api/comments/car/{carId}", carId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(commentsResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("车况很好");

        LocalDateTime cancelStart = end.plusDays(10);
        LocalDateTime cancelEnd = cancelStart.plusDays(1);
        String cancelOrderBody = """
                {
                  "carId": %d,
                  "pickupStoreId": %d,
                  "returnStoreId": %d,
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(carId, storeId, storeId, cancelStart, cancelEnd);
        MvcResult cancelOrderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelOrderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn();
        long cancelOrderId = objectMapper.readTree(cancelOrderResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/id").asLong();

        mockMvc.perform(put("/api/orders/{orderId}/cancel", cancelOrderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/token").asText();
    }
}
