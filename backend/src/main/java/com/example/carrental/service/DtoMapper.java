package com.example.carrental.service;

import com.example.carrental.domain.*;
import com.example.carrental.dto.*;

import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserDtos.UserResponse toUserResponse(User user) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                maskPhone(user.getPhone()),
                user.getEmail(),
                user.getRealName(),
                maskId(user.getIdCard()),
                maskId(user.getDriverLicenseNo()),
                user.getStatus(),
                user.getRole()
        );
    }

    public static StoreDtos.StoreResponse toStoreResponse(Store store) {
        return new StoreDtos.StoreResponse(
                store.getId(),
                store.getStoreName(),
                store.getCity(),
                store.getAddress(),
                store.getPhone(),
                store.getBusinessHours(),
                store.getStatus()
        );
    }

    public static CarDtos.CategoryResponse toCategoryResponse(CarCategory category) {
        if (category == null) {
            return null;
        }
        return new CarDtos.CategoryResponse(category.getId(), category.getCategoryName(), category.getDescription());
    }

    public static CarDtos.CarResponse toCarResponse(Car car) {
        List<String> images = car.getImages().stream().map(CarImage::getImageUrl).toList();
        return new CarDtos.CarResponse(
                car.getId(),
                car.getCarName(),
                car.getBrand(),
                car.getModel(),
                toCategoryResponse(car.getCategory()),
                car.getPlateNumber(),
                toStoreResponse(car.getStore()),
                car.getPricePerDay(),
                car.getDeposit(),
                car.getStatus(),
                car.getMileage(),
                car.getDescription(),
                images
        );
    }

    public static OrderDtos.OrderResponse toOrderResponse(RentalOrder order) {
        return new OrderDtos.OrderResponse(
                order.getId(),
                order.getOrderNo(),
                toUserResponse(order.getUser()),
                toCarResponse(order.getCar()),
                toStoreResponse(order.getPickupStore()),
                toStoreResponse(order.getReturnStore()),
                order.getStartTime(),
                order.getEndTime(),
                order.getRentalDays(),
                order.getTotalAmount(),
                order.getDepositAmount(),
                order.getStatus()
        );
    }

    public static PaymentDtos.PaymentResponse toPaymentResponse(PaymentOrder payment) {
        return new PaymentDtos.PaymentResponse(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getRentalOrder().getId(),
                payment.getUser().getId(),
                payment.getPayAmount(),
                payment.getPayType(),
                payment.getPayStatus(),
                payment.getTransactionNo(),
                payment.getPayTime()
        );
    }

    public static ContractDtos.ContractResponse toContractResponse(Contract contract) {
        return new ContractDtos.ContractResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getRentalOrder().getId(),
                contract.getUser().getId(),
                contract.getContractUrl(),
                contract.getSignStatus()
        );
    }

    public static CommentDtos.CommentResponse toCommentResponse(Comment comment) {
        return new CommentDtos.CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getCar().getId(),
                comment.getRentalOrder().getId(),
                comment.getScore(),
                comment.getContent(),
                comment.getStatus(),
                comment.getCreateTime()
        );
    }

    public static MaintenanceDtos.MaintenanceResponse toMaintenanceResponse(MaintenanceRecord record) {
        return new MaintenanceDtos.MaintenanceResponse(
                record.getId(),
                record.getCar().getId(),
                record.getType(),
                record.getDescription(),
                record.getCost(),
                record.getRecordTime()
        );
    }

    private static String maskPhone(String value) {
        if (value == null || value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private static String maskId(String value) {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
