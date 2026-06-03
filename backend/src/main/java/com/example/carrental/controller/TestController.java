package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.CommentRepository;
import com.example.carrental.repository.ContractRepository;
import com.example.carrental.repository.MaintenanceRecordRepository;
import com.example.carrental.repository.PaymentOrderRepository;
import com.example.carrental.repository.RentalOrderRepository;
import com.example.carrental.repository.StoreRepository;
import com.example.carrental.repository.UserRepository;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.CurrentUser;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/test")
public class TestController {

    private static final String CONTROLLER_PROFILE = "dev,test";

    private final Environment environment;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final StoreRepository storeRepository;
    private final RentalOrderRepository orderRepository;
    private final PaymentOrderRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final CommentRepository commentRepository;
    private final MaintenanceRecordRepository maintenanceRepository;

    public TestController(
            Environment environment,
            UserRepository userRepository,
            CarRepository carRepository,
            StoreRepository storeRepository,
            RentalOrderRepository orderRepository,
            PaymentOrderRepository paymentRepository,
            ContractRepository contractRepository,
            CommentRepository commentRepository,
            MaintenanceRecordRepository maintenanceRepository
    ) {
        this.environment = environment;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.commentRepository = commentRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @PublicEndpoint
    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.ok(new PingResponse(
                "UP",
                activeProfiles(),
                CONTROLLER_PROFILE,
                OffsetDateTime.now()
        ));
    }

    @GetMapping("/auth/me")
    public ApiResponse<AuthProbeResponse> currentUser() {
        CurrentUser currentUser = AuthContext.required();
        return ApiResponse.ok(new AuthProbeResponse(
                currentUser.id(),
                currentUser.username(),
                currentUser.role().name()
        ));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/auth/admin-only")
    public ApiResponse<RoleProbeResponse> adminOnly() {
        CurrentUser currentUser = AuthContext.required();
        return ApiResponse.ok(new RoleProbeResponse(
                true,
                UserRole.ADMIN.name(),
                currentUser.username()
        ));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/database")
    public ApiResponse<DatabaseProbeResponse> database() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", userRepository.count());
        counts.put("cars", carRepository.count());
        counts.put("stores", storeRepository.count());
        counts.put("orders", orderRepository.count());
        counts.put("payments", paymentRepository.count());
        counts.put("contracts", contractRepository.count());
        counts.put("comments", commentRepository.count());
        counts.put("maintenanceRecords", maintenanceRepository.count());

        List<CheckResult> checks = counts.entrySet().stream()
                .map(entry -> new CheckResult(entry.getKey(), entry.getValue() >= 0 ? "PASS" : "FAIL", entry.getValue()))
                .toList();

        return ApiResponse.ok(new DatabaseProbeResponse(true, counts, checks, OffsetDateTime.now()));
    }

    private List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return Arrays.asList(profiles);
    }

    public record PingResponse(
            String status,
            List<String> activeProfiles,
            String controllerProfile,
            OffsetDateTime checkedAt
    ) {
    }

    public record AuthProbeResponse(Long id, String username, String role) {
    }

    public record RoleProbeResponse(boolean allowed, String requiredRole, String username) {
    }

    public record DatabaseProbeResponse(
            boolean databaseConnected,
            Map<String, Long> counts,
            List<CheckResult> checks,
            OffsetDateTime checkedAt
    ) {
    }

    public record CheckResult(String name, String status, long count) {
    }
}
