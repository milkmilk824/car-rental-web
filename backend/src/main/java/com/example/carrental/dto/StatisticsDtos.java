package com.example.carrental.dto;

import java.math.BigDecimal;
import java.util.List;

public final class StatisticsDtos {

    private StatisticsDtos() {
    }

    public record DashboardResponse(
            long todayOrders,
            BigDecimal monthRevenue,
            double rentalRate,
            long activeUsers,
            long availableCars,
            long rentingOrders,
            List<HotCarResponse> hotCars,
            List<StorePerformanceResponse> storePerformance
    ) {
    }

    public record HotCarResponse(Long carId, String carName, long orderCount) {
    }

    public record StorePerformanceResponse(Long storeId, String storeName, long orderCount) {
    }

    public record RevenueTrendResponse(String date, BigDecimal revenue) {
    }
}
