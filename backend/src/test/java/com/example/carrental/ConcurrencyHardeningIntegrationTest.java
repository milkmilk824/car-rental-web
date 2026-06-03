package com.example.carrental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.carrental.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.login.limit=2",
        "app.rate-limit.window=PT1M"
})
@AutoConfigureMockMvc
class ConcurrencyHardeningIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentService paymentService;

    @Test
    void duplicateRefundRequestIsIdempotent() throws Exception {
        String userToken = login("zhangsan", "123456", "198.51.100.10").get("token").asText();
        String adminToken = login("admin", "123456", "198.51.100.11").get("token").asText();
        JsonNode car = firstAvailableCar();
        long carId = car.get("id").asLong();
        long storeId = car.at("/store/id").asLong();
        long orderId = createOrder(userToken, carId, storeId);
        JsonNode payment = createPayment(userToken, orderId, "refund-idem-key");
        String paymentNo = payment.get("paymentNo").asText();

        mockMvc.perform(post("/api/payments/{paymentNo}/simulate-success", paymentNo)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("SUCCESS"));

        String refundBody = """
                {"paymentNo": "%s", "reason": "duplicate refund test"}
                """.formatted(paymentNo);
        mockMvc.perform(post("/api/payments/refund")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "refund-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("REFUNDED"));

        mockMvc.perform(post("/api/payments/refund")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", "refund-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("REFUNDED"));
    }

    @Test
    void callbackIdempotencyKeyCannotBeReusedByDifferentPayment() throws Exception {
        String userToken = login("zhangsan", "123456", "198.51.100.12").get("token").asText();

        JsonNode firstCar = firstAvailableCar();
        long firstOrderId = createOrder(userToken, firstCar.get("id").asLong(), firstCar.at("/store/id").asLong());
        JsonNode firstPayment = createPayment(userToken, firstOrderId, "callback-payment-key-001");
        String firstPaymentNo = firstPayment.get("paymentNo").asText();

        mockMvc.perform(post("/api/payments/callback")
                        .header("Idempotency-Key", "callback-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(firstPayment, "TX-CALLBACK-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("SUCCESS"));

        JsonNode secondCar = firstAvailableCar();
        long secondOrderId = createOrder(userToken, secondCar.get("id").asLong(), secondCar.at("/store/id").asLong());
        JsonNode secondPayment = createPayment(userToken, secondOrderId, "callback-payment-key-002");

        mockMvc.perform(post("/api/payments/callback")
                        .header("Idempotency-Key", "callback-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(secondPayment, "TX-CALLBACK-002")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("支付回调幂等键已被其他支付单使用"));

        mockMvc.perform(get("/api/payments/status/{paymentNo}", firstPaymentNo)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payStatus").value("SUCCESS"));

        cancelOrder(userToken, firstOrderId);
        cancelOrder(userToken, secondOrderId);
    }

    @Test
    void loginEndpointIsRateLimitedByClientIdentity() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/user/login")
                            .header("X-Forwarded-For", "203.0.113.200")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "zhangsan", "password": "wrong-password"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/user/login")
                        .header("X-Forwarded-For", "203.0.113.200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "zhangsan", "password": "wrong-password"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("请求过于频繁，请稍后再试"));
    }

    private JsonNode login(String username, String password, String ip) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }

    private JsonNode firstAvailableCar() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cars")
                        .param("status", "AVAILABLE")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/items/0");
    }

    private long createOrder(String userToken, long carId, long storeId) throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(20).withNano(0);
        LocalDateTime end = start.plusDays(2);
        MvcResult result = mockMvc.perform(post("/api/orders")
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
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).at("/data/id").asLong();
    }

    private JsonNode createPayment(String userToken, long orderId, String idempotencyKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", "Bearer " + userToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": %d, "payType": "MOCK"}
                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payAmount").exists())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("data");
    }

    private void cancelOrder(String userToken, long orderId) throws Exception {
        mockMvc.perform(put("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    private String callbackBody(JsonNode payment, String transactionNo) {
        String paymentNo = payment.get("paymentNo").asText();
        String amount = payment.get("payAmount").asText();
        return """
                {
                  "paymentNo": "%s",
                  "transactionNo": "%s",
                  "payAmount": %s,
                  "signature": "%s"
                }
                """.formatted(
                paymentNo,
                transactionNo,
                amount,
                paymentService.expectedSignature(paymentNo, payment.get("payAmount").decimalValue())
        );
    }
}
