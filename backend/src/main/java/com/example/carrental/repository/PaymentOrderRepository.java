package com.example.carrental.repository;

import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.domain.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Page<PaymentOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<PaymentOrder> findByPaymentNo(String paymentNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    @Query("select p from PaymentOrder p where p.paymentNo = :paymentNo")
    Optional<PaymentOrder> findByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<PaymentOrder> findByRentalOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    @Query("select p from PaymentOrder p where p.rentalOrder.id = :orderId")
    Optional<PaymentOrder> findByRentalOrderIdForUpdate(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<PaymentOrder> findByCallbackIdempotencyKey(String callbackIdempotencyKey);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<PaymentOrder> findByRefundIdempotencyKey(String refundIdempotencyKey);

    List<PaymentOrder> findByPayStatus(PayStatus status);
}
