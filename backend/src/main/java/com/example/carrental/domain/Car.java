package com.example.carrental.domain;

import com.example.carrental.common.Enums.CarStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "car", indexes = {
        @Index(name = "idx_car_brand", columnList = "brand"),
        @Index(name = "idx_car_status", columnList = "status"),
        @Index(name = "idx_car_brand_status", columnList = "brand,status"),
        @Index(name = "idx_car_store_status", columnList = "store_id,status"),
        @Index(name = "idx_car_category_status", columnList = "category_id,status"),
        @Index(name = "idx_car_status_create_time", columnList = "status,create_time")
})
public class Car extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String carName;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CarCategory category;

    @Column(nullable = false, length = 30)
    private String plateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarStatus status = CarStatus.AVAILABLE;

    private Integer mileage = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Version
    private Long version;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<CarImage> images = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public CarCategory getCategory() {
        return category;
    }

    public void setCategory(CarCategory category) {
        this.category = category;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public CarStatus getStatus() {
        return status;
    }

    public void setStatus(CarStatus status) {
        this.status = status;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<CarImage> getImages() {
        return images;
    }

    public void setImages(List<CarImage> images) {
        this.images = images;
    }

    public void replaceImages(List<String> imageUrls) {
        images.clear();
        if (imageUrls == null) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            CarImage image = new CarImage();
            image.setCar(this);
            image.setImageUrl(imageUrls.get(i));
            image.setMain(i == 0);
            images.add(image);
        }
    }
}
