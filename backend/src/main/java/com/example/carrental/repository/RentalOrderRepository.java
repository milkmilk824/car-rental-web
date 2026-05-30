package com.example.carrental.repository;

import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.domain.RentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    Optional<RentalOrder> findByOrderNo(String orderNo);

    List<RentalOrder> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<RentalOrder> findByPickupStoreIdOrReturnStoreIdOrderByCreateTimeDesc(Long pickupStoreId, Long returnStoreId);

    long countByStatus(OrderStatus status);

    long countByStatusIn(Collection<OrderStatus> statuses);

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);
}
