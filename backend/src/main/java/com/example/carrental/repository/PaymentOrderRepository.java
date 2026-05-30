package com.example.carrental.repository;

import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.domain.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByPaymentNo(String paymentNo);

    Optional<PaymentOrder> findByRentalOrderId(Long orderId);

    List<PaymentOrder> findByPayStatus(PayStatus status);
}
