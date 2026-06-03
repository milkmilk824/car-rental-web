package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.PageResult;
import com.example.carrental.domain.PaymentOrder;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.dto.PaymentDtos;
import com.example.carrental.repository.PaymentOrderRepository;
import com.example.carrental.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentOrderRepository paymentRepository;
    private final OrderService orderService;
    private final ContractService contractService;
    private final HotCacheService cacheService;
    private final String callbackSecret;

    public PaymentService(
            PaymentOrderRepository paymentRepository,
            OrderService orderService,
            ContractService contractService,
            HotCacheService cacheService,
            @Value("${app.payment.callback-secret}") String callbackSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.contractService = contractService;
        this.cacheService = cacheService;
        this.callbackSecret = callbackSecret;
    }

    public PaymentDtos.PaymentResponse create(PaymentDtos.CreatePaymentRequest request, Long currentUserId, String idempotencyKey) {
        String normalizedKey = normalizeKey(idempotencyKey);
        if (normalizedKey != null) {
            PaymentOrder existing = paymentRepository.findByIdempotencyKey(normalizedKey).orElse(null);
            if (existing != null) {
                if (!existing.getUser().getId().equals(currentUserId)) {
                    throw BusinessException.forbidden("不能复用他人支付幂等键");
                }
                return DtoMapper.toPaymentResponse(existing);
            }
        }
        RentalOrder order = orderService.findById(request.orderId());
        if (!order.getUser().getId().equals(currentUserId)) {
            throw BusinessException.forbidden("不能为他人订单创建支付单");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("订单当前不需要支付");
        }
        PaymentOrder payment = paymentRepository.findByRentalOrderIdForUpdate(order.getId()).orElseGet(PaymentOrder::new);
        if (payment.getId() == null) {
            payment.setPaymentNo("PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            payment.setRentalOrder(order);
            payment.setUser(order.getUser());
            payment.setPayAmount(order.getTotalAmount().add(order.getDepositAmount()));
            payment.setPayType(request.payType());
            payment.setPayStatus(PayStatus.WAITING);
            payment.setIdempotencyKey(normalizedKey);
            paymentRepository.save(payment);
        } else if (payment.getIdempotencyKey() == null && normalizedKey != null) {
            payment.setIdempotencyKey(normalizedKey);
        }
        return DtoMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse status(String paymentNo) {
        return DtoMapper.toPaymentResponse(findByPaymentNo(paymentNo));
    }

    @Transactional(readOnly = true)
    public PageResult<PaymentDtos.PaymentResponse> list(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        return PageResult.from(paymentRepository.findAll(pageRequest).map(DtoMapper::toPaymentResponse));
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

    public PaymentDtos.PaymentResponse callback(PaymentDtos.PaymentCallbackRequest request, String idempotencyKey) {
        String normalizedKey = normalizeKey(idempotencyKey);
        PaymentOrder payment = findByPaymentNoForUpdate(request.paymentNo());
        if (!expectedSignature(request.paymentNo(), request.payAmount()).equalsIgnoreCase(request.signature())) {
            throw BusinessException.forbidden("支付回调签名校验失败");
        }
        bindCallbackIdempotencyKey(payment, normalizedKey);
        return markSuccess(payment, request.transactionNo(), request.payAmount());
    }

    public PaymentDtos.PaymentResponse simulateSuccess(String paymentNo, CurrentUser currentUser) {
        PaymentOrder payment = findByPaymentNoForUpdate(paymentNo);
        if (currentUser.role() != UserRole.ADMIN && !payment.getUser().getId().equals(currentUser.id())) {
            throw BusinessException.forbidden("不能操作他人支付单");
        }
        return markSuccess(payment, "MOCK-" + UUID.randomUUID(), payment.getPayAmount());
    }

    public PaymentDtos.PaymentResponse refund(PaymentDtos.RefundRequest request, String idempotencyKey) {
        String normalizedKey = normalizeKey(idempotencyKey);
        PaymentOrder existingByKey = findByRefundIdempotencyKey(normalizedKey);
        if (existingByKey != null) {
            if (!existingByKey.getPaymentNo().equals(request.paymentNo())) {
                throw BusinessException.forbidden("退款幂等键已被其他支付单使用");
            }
            return DtoMapper.toPaymentResponse(existingByKey);
        }
        PaymentOrder payment = findByPaymentNoForUpdate(request.paymentNo());
        if (payment.getPayStatus() == PayStatus.REFUNDED) {
            bindRefundIdempotencyKey(payment, normalizedKey);
            return DtoMapper.toPaymentResponse(payment);
        }
        if (payment.getPayStatus() != PayStatus.SUCCESS && payment.getPayStatus() != PayStatus.REFUNDING) {
            throw BusinessException.badRequest("当前支付状态不能退款");
        }
        bindRefundIdempotencyKey(payment, normalizedKey);
        payment.setPayStatus(PayStatus.REFUNDED);
        payment.setRefundReason(request.reason());
        payment.setRefundTime(LocalDateTime.now());
        RentalOrder order = payment.getRentalOrder();
        order.setStatus(OrderStatus.REFUNDED);
        order.getCar().setStatus(com.example.carrental.common.Enums.CarStatus.AVAILABLE);
        evictStats();
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
        evictStats();
        return DtoMapper.toPaymentResponse(payment);
    }

    private PaymentOrder findByPaymentNo(String paymentNo) {
        return paymentRepository.findByPaymentNo(paymentNo).orElseThrow(() -> BusinessException.notFound("支付单不存在"));
    }

    private PaymentOrder findByPaymentNoForUpdate(String paymentNo) {
        return paymentRepository.findByPaymentNoForUpdate(paymentNo).orElseThrow(() -> BusinessException.notFound("支付单不存在"));
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim().length() > 80 ? idempotencyKey.trim().substring(0, 80) : idempotencyKey.trim();
    }

    private void bindCallbackIdempotencyKey(PaymentOrder payment, String normalizedKey) {
        if (normalizedKey == null) {
            return;
        }
        PaymentOrder existing = paymentRepository.findByCallbackIdempotencyKey(normalizedKey).orElse(null);
        if (existing != null && !existing.getId().equals(payment.getId())) {
            throw BusinessException.forbidden("支付回调幂等键已被其他支付单使用");
        }
        if (payment.getCallbackIdempotencyKey() == null) {
            payment.setCallbackIdempotencyKey(normalizedKey);
            flushIdempotency("支付回调幂等键已被其他支付单使用");
        }
    }

    private PaymentOrder findByRefundIdempotencyKey(String normalizedKey) {
        if (normalizedKey == null) {
            return null;
        }
        return paymentRepository.findByRefundIdempotencyKey(normalizedKey).orElse(null);
    }

    private void bindRefundIdempotencyKey(PaymentOrder payment, String normalizedKey) {
        if (normalizedKey == null) {
            return;
        }
        PaymentOrder existing = paymentRepository.findByRefundIdempotencyKey(normalizedKey).orElse(null);
        if (existing != null && !existing.getId().equals(payment.getId())) {
            throw BusinessException.forbidden("退款幂等键已被其他支付单使用");
        }
        if (payment.getRefundIdempotencyKey() == null) {
            payment.setRefundIdempotencyKey(normalizedKey);
            flushIdempotency("退款幂等键已被其他支付单使用");
        }
    }

    private void flushIdempotency(String duplicateMessage) {
        try {
            paymentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.forbidden(duplicateMessage);
        }
    }

    private void evictStats() {
        cacheService.evictPrefix("stats:");
        cacheService.evictPrefix("car:");
    }
}
