package com.example.carrental.service;

import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.PaymentOrder;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.domain.Store;
import com.example.carrental.dto.StatisticsDtos;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.PaymentOrderRepository;
import com.example.carrental.repository.RentalOrderRepository;
import com.example.carrental.repository.StoreRepository;
import com.example.carrental.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final RentalOrderRepository orderRepository;
    private final PaymentOrderRepository paymentRepository;
    private final CarRepository carRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public StatisticsService(
            RentalOrderRepository orderRepository,
            PaymentOrderRepository paymentRepository,
            CarRepository carRepository,
            StoreRepository storeRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.carRepository = carRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    public StatisticsDtos.DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        long todayOrders = orderRepository.countByCreateTimeBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime monthEnd = firstDay.plusMonths(1).atStartOfDay();
        BigDecimal monthRevenue = paymentRepository.findByPayStatus(PayStatus.SUCCESS).stream()
                .filter(payment -> payment.getPayTime() != null)
                .filter(payment -> !payment.getPayTime().isBefore(monthStart) && payment.getPayTime().isBefore(monthEnd))
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCars = carRepository.count();
        long rentingCars = carRepository.countByStatus(CarStatus.RENTING);
        double rentalRate = totalCars == 0 ? 0 : (rentingCars * 100.0d / totalCars);

        List<RentalOrder> orders = orderRepository.findAll();
        Map<Long, Long> carCount = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCar().getId(), Collectors.counting()));
        Map<Long, Car> carMap = carRepository.findAll().stream().collect(Collectors.toMap(Car::getId, car -> car));
        List<StatisticsDtos.HotCarResponse> hotCars = carCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new StatisticsDtos.HotCarResponse(entry.getKey(), carMap.get(entry.getKey()).getCarName(), entry.getValue()))
                .toList();

        Map<Long, Long> storeCount = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getPickupStore().getId(), Collectors.counting()));
        Map<Long, Store> storeMap = storeRepository.findAll().stream().collect(Collectors.toMap(Store::getId, store -> store));
        List<StatisticsDtos.StorePerformanceResponse> stores = storeCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new StatisticsDtos.StorePerformanceResponse(entry.getKey(), storeMap.get(entry.getKey()).getStoreName(), entry.getValue()))
                .toList();

        return new StatisticsDtos.DashboardResponse(
                todayOrders,
                monthRevenue,
                rentalRate,
                userRepository.count(),
                carRepository.countByStatus(CarStatus.AVAILABLE),
                orderRepository.countByStatus(OrderStatus.RENTING),
                hotCars,
                stores
        );
    }
}
