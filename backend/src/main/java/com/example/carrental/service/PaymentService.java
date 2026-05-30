package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.domain.PaymentOrder;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.dto.PaymentDtos;
import com.example.carrental.repository.PaymentOrderRepository;
import com.example.carrental.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentOrderRepository paymentRepository;
    private final OrderService orderService;
    private final ContractService contractService;
    private final String callbackSecret;

    public PaymentService(
            PaymentOrderRepository paymentRepository,
            OrderService orderService,
            ContractService contractService,
            @Value("${app.payment.callback-secret}") String callbackSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.contractService = contractService;
        this.callbackSecret = callbackSecret;
    }

    public PaymentDtos.PaymentResponse create(PaymentDtos.CreatePaymentRequest request, Long currentUserId) {
        RentalOrder order = orderService.findById(request.orderId());
        if (!order.getUser().getId().equals(currentUserId)) {
            throw BusinessException.forbidden("不能为他人订单创建支付单");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("订单当前不需要支付");
        }
        PaymentOrder payment = paymentRepository.findByRentalOrderId(order.getId()).orElseGet(PaymentOrder::new);
        if (payment.getId() == null) {
            payment.setPaymentNo("PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            payment.setRentalOrder(order);
            payment.setUser(order.getUser());
            payment.setPayAmount(order.getTotalAmount().add(order.getDepositAmount()));
            payment.setPayType(request.payType());
            payment.setPayStatus(PayStatus.WAITING);
            paymentRepository.save(payment);
        }
        return DtoMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse status(String paymentNo) {
        return DtoMapper.toPaymentResponse(findByPaymentNo(paymentNo));
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.PaymentResponse> list() {
        return paymentRepository.findAll().stream().map(DtoMapper::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse byOrder(Long orderId, Long currentUserId) {
        PaymentOrder payment = paymentRepository.findByRentalOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("该订单无支付记录"));
        if (!payment.getUser().getId().equals(currentUserId)) {
            throw BusinessException.forbidden("不能查看他人支付记录");
        }
        return DtoMapper.toPaymentResponse(payment);
    }

    public PaymentDtos.PaymentResponse callback(PaymentDtos.PaymentCallbackRequest request) {
        PaymentOrder payment = findByPaymentNo(request.paymentNo());
        if (!expectedSignature(request.paymentNo(), request.payAmount()).equalsIgnoreCase(request.signature())) {
            throw BusinessException.forbidden("支付回调签名校验失败");
        }
        return markSuccess(payment, request.transactionNo(), request.payAmount());
    }

    public PaymentDtos.PaymentResponse simulateSuccess(String paymentNo, CurrentUser currentUser) {
        PaymentOrder payment = findByPaymentNo(paymentNo);
        if (currentUser.role() != UserRole.ADMIN && !payment.getUser().getId().equals(currentUser.id())) {
            throw BusinessException.forbidden("不能操作他人支付单");
        }
        return markSuccess(payment, "MOCK-" + UUID.randomUUID(), payment.getPayAmount());
    }

    public PaymentDtos.PaymentResponse refund(PaymentDtos.RefundRequest request) {
        PaymentOrder payment = findByPaymentNo(request.paymentNo());
        if (payment.getPayStatus() != PayStatus.SUCCESS && payment.getPayStatus() != PayStatus.REFUNDING) {
            throw BusinessException.badRequest("当前支付状态不能退款");
        }
        payment.setPayStatus(PayStatus.REFUNDED);
        RentalOrder order = payment.getRentalOrder();
        order.setStatus(OrderStatus.REFUNDED);
        order.getCar().setStatus(com.example.carrental.common.Enums.CarStatus.AVAILABLE);
        return DtoMapper.toPaymentResponse(payment);
    }

    public String expectedSignature(String paymentNo, BigDecimal amount) {
        String raw = paymentNo + ":" + amount.toPlainString() + ":" + callbackSecret;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PaymentDtos.PaymentResponse markSuccess(PaymentOrder payment, String transactionNo, BigDecimal amount) {
        if (payment.getPayStatus() == PayStatus.SUCCESS) {
            return DtoMapper.toPaymentResponse(payment);
        }
        if (payment.getPayAmount().compareTo(amount) != 0) {
            throw BusinessException.badRequest("支付金额与订单金额不一致");
        }
        payment.setPayStatus(PayStatus.SUCCESS);
        payment.setTransactionNo(transactionNo);
        payment.setPayTime(LocalDateTime.now());
        RentalOrder order = orderService.markPaid(payment.getRentalOrder().getId());
        contractService.generateInternal(order.getId());
        return DtoMapper.toPaymentResponse(payment);
    }

    private PaymentOrder findByPaymentNo(String paymentNo) {
        return paymentRepository.findByPaymentNo(paymentNo).orElseThrow(() -> BusinessException.notFound("支付单不存在"));
    }
}
