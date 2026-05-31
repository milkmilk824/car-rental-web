package com.example.carrental.repository;

import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.domain.RentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select count(o)
            from RentalOrder o
            where o.car.id = :carId
              and o.status in :statuses
              and o.startTime < :endTime
              and o.endTime > :startTime
            """)
    long countOverlappingReservations(
            @Param("carId") Long carId,
            @Param("statuses") Collection<OrderStatus> statuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
