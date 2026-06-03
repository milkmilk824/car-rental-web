package com.example.carrental.repository;

import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.domain.RentalOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"car", "pickupStore"})
    List<RentalOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "car", "car.category", "car.store", "pickupStore", "returnStore"})
    Page<RentalOrder> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "car", "car.category", "car.store", "car.images", "pickupStore", "returnStore"})
    Optional<RentalOrder> findById(Long id);

    @EntityGraph(attributePaths = {"user", "car", "car.category", "car.store", "car.images", "pickupStore", "returnStore"})
    Optional<RentalOrder> findByOrderNo(String orderNo);

    @EntityGraph(attributePaths = {"user", "car", "car.category", "car.store", "pickupStore", "returnStore"})
    List<RentalOrder> findByUserIdOrderByCreateTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "car", "car.category", "car.store", "pickupStore", "returnStore"})
    List<RentalOrder> findByPickupStoreIdOrReturnStoreIdOrderByCreateTimeDesc(Long pickupStoreId, Long returnStoreId);

    @EntityGraph(attributePaths = {"user", "car", "car.category", "car.store", "pickupStore", "returnStore"})
    Page<RentalOrder> findByPickupStoreIdOrReturnStoreId(Long pickupStoreId, Long returnStoreId, Pageable pageable);

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
