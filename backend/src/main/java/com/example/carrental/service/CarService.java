package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.common.PageResult;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarCategory;
import com.example.carrental.dto.CarDtos;
import com.example.carrental.repository.CarCategoryRepository;
import com.example.carrental.repository.CarRepository;
import com.example.carrental.repository.RentalOrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CarService {

    private final CarRepository carRepository;
    private final CarCategoryRepository categoryRepository;
    private final StoreService storeService;
    private final RentalOrderRepository orderRepository;
    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = EnumSet.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.PENDING_PICKUP,
            OrderStatus.RENTING,
            OrderStatus.PENDING_RETURN,
            OrderStatus.REFUNDING,
            OrderStatus.EXCEPTION
    );

    public CarService(
            CarRepository carRepository,
            CarCategoryRepository categoryRepository,
            StoreService storeService,
            RentalOrderRepository orderRepository
    ) {
        this.carRepository = carRepository;
        this.categoryRepository = categoryRepository;
        this.storeService = storeService;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<CarDtos.CarResponse> search(
            String keyword,
            String brand,
            Long categoryId,
            Long storeId,
            String city,
            CarStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size
    ) {
        Specification<Car> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("carName")), like),
                        cb.like(cb.lower(root.get("brand")), like),
                        cb.like(cb.lower(root.get("model")), like)
                ));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("store").get("city")), "%" + city.trim().toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createTime"));
        return PageResult.from(carRepository.findAll(spec, pageable).map(DtoMapper::toCarResponse));
    }

    @Transactional(readOnly = true)
    public CarDtos.CarResponse detail(Long id) {
        return DtoMapper.toCarResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public CarDtos.AvailabilityResponse availability(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        validateRentalWindow(startTime, endTime);
        Car car = findById(id);
        if (car.getStatus() != CarStatus.AVAILABLE) {
            return new CarDtos.AvailabilityResponse(id, false, "车辆当前状态不可租");
        }
        if (hasOverlappingReservation(id, startTime, endTime)) {
            return new CarDtos.AvailabilityResponse(id, false, "该时间段已被预订");
        }
        return new CarDtos.AvailabilityResponse(id, true, "可租");
    }

    @Transactional(readOnly = true)
    public boolean hasOverlappingReservation(Long carId, LocalDateTime startTime, LocalDateTime endTime) {
        validateRentalWindow(startTime, endTime);
        return orderRepository.countOverlappingReservations(carId, ACTIVE_ORDER_STATUSES, startTime, endTime) > 0;
    }

    @Transactional(readOnly = true)
    public List<CarDtos.CategoryResponse> categories() {
        return categoryRepository.findAll().stream().map(DtoMapper::toCategoryResponse).toList();
    }

    public CarDtos.CategoryResponse createCategory(CarDtos.CategoryRequest request) {
        categoryRepository.findByCategoryName(request.categoryName()).ifPresent(existing -> {
            throw BusinessException.badRequest("车辆分类已存在");
        });
        CarCategory category = new CarCategory();
        category.setCategoryName(request.categoryName());
        category.setDescription(request.description());
        categoryRepository.save(category);
        return DtoMapper.toCategoryResponse(category);
    }

    public CarDtos.CategoryResponse updateCategory(Long id, CarDtos.CategoryRequest request) {
        CarCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("车辆分类不存在"));
        if (!category.getCategoryName().equals(request.categoryName())) {
            categoryRepository.findByCategoryName(request.categoryName()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw BusinessException.badRequest("车辆分类已存在");
                }
            });
        }
        category.setCategoryName(request.categoryName());
        category.setDescription(request.description());
        return DtoMapper.toCategoryResponse(category);
    }

    public void deleteCategory(Long id) {
        CarCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("车辆分类不存在"));
        categoryRepository.delete(category);
    }

    public CarDtos.CarResponse create(CarDtos.CarRequest request) {
        Car car = new Car();
        apply(car, request);
        carRepository.save(car);
        return DtoMapper.toCarResponse(car);
    }

    public CarDtos.CarResponse update(Long id, CarDtos.CarRequest request) {
        Car car = findById(id);
        apply(car, request);
        return DtoMapper.toCarResponse(car);
    }

    public CarDtos.CarResponse updateStatus(Long id, CarStatus status) {
        Car car = findById(id);
        car.setStatus(status);
        return DtoMapper.toCarResponse(car);
    }

    public void delete(Long id) {
        Car car = findById(id);
        carRepository.delete(car);
    }

    public Car findById(Long id) {
        return carRepository.findById(id).orElseThrow(() -> BusinessException.notFound("车辆不存在"));
    }

    public Car findByIdForUpdate(Long id) {
        return carRepository.findByIdForUpdate(id).orElseThrow(() -> BusinessException.notFound("车辆不存在"));
    }

    private void validateRentalWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw BusinessException.badRequest("请选择取还车时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw BusinessException.badRequest("还车时间必须晚于取车时间");
        }
    }

    private void apply(Car car, CarDtos.CarRequest request) {
        CarCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> BusinessException.notFound("车辆分类不存在"));
        car.setCarName(request.carName());
        car.setBrand(request.brand());
        car.setModel(request.model());
        car.setCategory(category);
        car.setPlateNumber(request.plateNumber());
        car.setStore(storeService.findById(request.storeId()));
        car.setPricePerDay(request.pricePerDay());
        car.setDeposit(request.deposit());
        car.setStatus(request.status());
        car.setMileage(request.mileage() == null ? 0 : request.mileage());
        car.setDescription(request.description());
        car.replaceImages(request.imageUrls());
    }
}
