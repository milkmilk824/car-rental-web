package com.example.carrental.common;

public final class Enums {

    private Enums() {
    }

    public enum UserRole {
        USER, STORE_STAFF, ADMIN
    }

    public enum UserStatus {
        ACTIVE, DISABLED
    }

    public enum StoreStatus {
        OPEN, CLOSED
    }

    public enum CarStatus {
        AVAILABLE, RESERVED, RENTING, REPAIRING, MAINTAINING, OFFLINE
    }

    public enum OrderStatus {
        PENDING_PAYMENT, PENDING_PICKUP, RENTING, PENDING_RETURN, COMPLETED,
        CANCELLED, REFUNDING, REFUNDED, EXCEPTION
    }

    public enum PayType {
        ALIPAY, WECHAT, BANK_CARD, CASH, MOCK
    }

    public enum PayStatus {
        WAITING, SUCCESS, REFUNDING, REFUNDED, CLOSED
    }

    public enum ContractStatus {
        UNSIGNED, SIGNED, ARCHIVED
    }

    public enum CommentStatus {
        PENDING, APPROVED, REMOVED
    }

    public enum MaintenanceType {
        REPAIR, MAINTENANCE
    }
}
