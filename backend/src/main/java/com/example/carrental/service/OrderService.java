package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.Enums.StoreStatus;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.PageResult;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.domain.Store;
import com.example.carrental.domain.User;
import com.example.carrental.dto.OrderDtos;
import com.example.carrental.repository.RentalOrderRepository;
import com.example.carrental.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final RentalOrderRepository orderRepository;
    private final UserService userService;
    private final CarService carService;
    private final StoreService storeService;
    private final StoreStaffService storeStaffService;
    private final HotCacheService cacheService;

    public OrderService(
            RentalOrderRepository orderRepository,
            UserService userService,
            CarService carService,
            StoreService storeService,
            StoreStaffService storeStaffService,
            HotCacheService cacheService
    ) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.carService = carService;
        this.storeService = storeService;
        this.storeStaffService = storeStaffService;
        this.cacheService = cacheService;
    }

    public OrderDtos.OrderResponse create(Long userId, OrderDtos.OrderCreateRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw BusinessException.badRequest("还车时间必须晚于取车时间");
        }
        User user = userService.findById(userId);
        Car car = carService.findByIdForUpdate(request.carId());
        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw BusinessException.badRequest("车辆当前不可租");
        }
        if (carService.hasOverlappingReservation(car.getId(), request.startTime(), request.endTime())) {
            throw BusinessException.badRequest("该时间段已被预订");
        }
        Store pickupStore = storeService.findById(request.pickupStoreId());
        Store returnStore = storeService.findById(request.returnStoreId());
        if (pickupStore.getStatus() != StoreStatus.OPEN || returnStore.getStatus() != StoreStatus.OPEN) {
            throw BusinessException.badRequest("取还车门店未营业");
        }

        int days = rentalDays(request.startTime(), request.endTime());
        RentalOrder order = new RentalOrder();
        order.setOrderNo("RO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setUser(user);
        order.setCar(car);
        order.setPickupStore(pickupStore);
        order.setReturnStore(returnStore);
        order.setStartTime(request.startTime());
        order.setEndTime(request.endTime());
        order.setRentalDays(days);
        order.setTotalAmount(car.getPricePerDay().multiply(BigDecimal.valueOf(days)));
        order.setDepositAmount(car.getDeposit());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        car.setStatus(CarStatus.RESERVED);
        orderRepository.save(order);
        evictOperationalCaches();
        return DtoMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> myOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .map(DtoMapper::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<OrderDtos.OrderResponse> allOrders(int page, int size) {
        return PageResult.from(orderRepository.findAll(pageRequest(page, size)).map(DtoMapper::toOrderResponse));
    }

    @Transactional(readOnly = true)
    public PageResult<OrderDtos.OrderResponse> storeOrders(Long storeId, CurrentUser currentUser, int page, int size) {
        storeStaffService.ensureStoreAccess(storeId, currentUser);
        return PageResult.from(orderRepository.findByPickupStoreIdOrReturnStoreId(storeId, storeId, pageRequest(page, size))
                .map(DtoMapper::toOrderResponse));
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse detail(Long orderId, CurrentUser currentUser) {
        RentalOrder order = findById(orderId);
        ensureVisible(order, currentUser);
        return DtoMapper.toOrderResponse(order);
    }

    public OrderDtos.OrderResponse cancel(Long orderId, CurrentUser currentUser) {
        RentalOrder order = findById(orderId);
        ensureVisible(order, currentUser);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PENDING_PICKUP) {
            throw BusinessException.badRequest("当前订单状态不允许取消");
        }
        order.setStatus(order.getStatus() == OrderStatus.PENDING_PICKUP ? OrderStatus.REFUNDING : OrderStatus.CANCELLED);
        order.getCar().setStatus(CarStatus.AVAILABLE);
        evictOperationalCaches();
        return DtoMapper.toOrderResponse(order);
    }

    public OrderDtos.OrderResponse renew(Long orderId, Long userId, int extraDays) {
        RentalOrder order = findById(orderId);
        if (!order.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("不能续租他人订单");
        }
        if (order.getStatus() != OrderStatus.RENTING && order.getStatus() != OrderStatus.PENDING_RETURN) {
            throw BusinessException.badRequest("仅租赁中订单可续租");
        }
        order.setEndTime(order.getEndTime().plusDays(extraDays));
        order.setRentalDays(order.getRentalDays() + extraDays);
        order.setTotalAmount(order.getCar().getPricePerDay().multiply(BigDecimal.valueOf(order.getRentalDays())));
        evictOperationalCaches();
        return DtoMapper.toOrderResponse(order);
    }

    public OrderDtos.OrderResponse confirmPickup(Long orderId, CurrentUser currentUser) {
        RentalOrder order = findById(orderId);
        storeStaffService.ensureStoreAccess(order.getPickupStore().getId(), currentUser);
        if (order.getStatus() != OrderStatus.PENDING_PICKUP) {
            throw BusinessException.badRequest("订单不是待取车状态");
        }
        order.setStatus(OrderStatus.RENTING);
        order.getCar().setStatus(CarStatus.RENTING);
        evictOperationalCaches();
        return DtoMapper.toOrderResponse(order);
    }

    public OrderDtos.OrderResponse confirmReturn(Long orderId, CurrentUser currentUser) {
        RentalOrder order = findById(orderId);
        storeStaffService.ensureStoreAccess(order.getReturnStore().getId(), currentUser);
        if (order.getStatus() != OrderStatus.RENTING && order.getStatus() != OrderStatus.PENDING_RETURN) {
            throw BusinessException.badRequest("订单不是租赁中状态");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.getCar().setStatus(CarStatus.AVAILABLE);
        evictOperationalCaches();
        return DtoMapper.toOrderResponse(order);
    }

    public RentalOrder markPaid(Long orderId) {
        RentalOrder order = findById(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return order;
        }
        order.setStatus(OrderStatus.PENDING_PICKUP);
        evictOperationalCaches();
        return order;
    }

    public RentalOrder findById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> BusinessException.notFound("订单不存在"));
    }

    private int rentalDays(LocalDateTime start, LocalDateTime end) {
        long minutes = ChronoUnit.MINUTES.between(start, end);
        return Math.max(1, (int) Math.ceil(minutes / 1440.0d));
    }

    private void ensureVisible(RentalOrder order, CurrentUser currentUser) {
        if (currentUser.role() == UserRole.ADMIN || currentUser.role() == UserRole.STORE_STAFF) {
            return;
        }
        if (!order.getUser().getId().equals(currentUser.id())) {
            throw BusinessException.forbidden("不能访问他人订单");
        }
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
    }

    private void evictOperationalCaches() {
        cacheService.evictPrefix("stats:");
        carService.evictCarCaches();
    }
}
