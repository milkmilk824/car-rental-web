package com.example.carrental.domain;

import com.example.carrental.common.Enums.StoreStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "store", indexes = {
        @Index(name = "idx_store_city_status", columnList = "city,status")
})
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String storeName;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String businessHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status = StoreStatus.OPEN;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public void setStatus(StoreStatus status) {
        this.status = status;
    }
}
