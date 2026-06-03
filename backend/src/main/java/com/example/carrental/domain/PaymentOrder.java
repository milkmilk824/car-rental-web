package com.example.carrental.domain;

import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.common.Enums.PayType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_order", indexes = {
        @Index(name = "idx_payment_no", columnList = "paymentNo", unique = true),
        @Index(name = "idx_payment_order", columnList = "order_id", unique = true),
        @Index(name = "idx_payment_status_time", columnList = "payStatus,payTime"),
        @Index(name = "idx_payment_user_status_time", columnList = "user_id,payStatus,payTime"),
        @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_payment_callback_idempotency", columnList = "callbackIdempotencyKey", unique = true),
        @Index(name = "idx_payment_refund_idempotency", columnList = "refundIdempotencyKey", unique = true)
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_order", columnNames = "order_id")
})
public class PaymentOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String paymentNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private RentalOrder rentalOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayType payType = PayType.MOCK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayStatus payStatus = PayStatus.WAITING;

    @Column(length = 100)
    private String transactionNo;

    @Column(length = 80, unique = true)
    private String idempotencyKey;

    @Column(length = 80, unique = true)
    private String callbackIdempotencyKey;

    @Column(length = 80, unique = true)
    private String refundIdempotencyKey;

    @Column(length = 255)
    private String refundReason;

    private LocalDateTime payTime;

    private LocalDateTime refundTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public RentalOrder getRentalOrder() {
        return rentalOrder;
    }

    public void setRentalOrder(RentalOrder rentalOrder) {
        this.rentalOrder = rentalOrder;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public PayType getPayType() {
        return payType;
    }

    public void setPayType(PayType payType) {
        this.payType = payType;
    }

    public PayStatus getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(PayStatus payStatus) {
        this.payStatus = payStatus;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRefundIdempotencyKey() {
        return refundIdempotencyKey;
    }

    public String getCallbackIdempotencyKey() {
        return callbackIdempotencyKey;
    }

    public void setCallbackIdempotencyKey(String callbackIdempotencyKey) {
        this.callbackIdempotencyKey = callbackIdempotencyKey;
    }

    public void setRefundIdempotencyKey(String refundIdempotencyKey) {
        this.refundIdempotencyKey = refundIdempotencyKey;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }
}
