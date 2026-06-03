package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.MaintenanceType;
import com.example.carrental.common.PageResult;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.MaintenanceRecord;
import com.example.carrental.dto.MaintenanceDtos;
import com.example.carrental.repository.MaintenanceRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MaintenanceService {

    private final MaintenanceRecordRepository recordRepository;
    private final CarService carService;

    public MaintenanceService(MaintenanceRecordRepository recordRepository, CarService carService) {
        this.recordRepository = recordRepository;
        this.carService = carService;
    }

    public MaintenanceDtos.MaintenanceResponse create(MaintenanceDtos.MaintenanceRequest request) {
        Car car = carService.findById(request.carId());
        MaintenanceRecord record = new MaintenanceRecord();
        record.setCar(car);
        record.setType(request.type());
        record.setDescription(request.description());
        record.setCost(request.cost() == null ? BigDecimal.ZERO : request.cost());
        record.setRecordTime(request.recordTime() == null ? LocalDateTime.now() : request.recordTime());
        car.setStatus(request.type() == MaintenanceType.REPAIR ? CarStatus.REPAIRING : CarStatus.MAINTAINING);
        recordRepository.save(record);
        carService.evictCarCaches();
        return DtoMapper.toMaintenanceResponse(record);
    }

    @Transactional(readOnly = true)
    public PageResult<MaintenanceDtos.MaintenanceResponse> listAll(int page, int size) {
        PageRequest pageRequest = pageRequest(page, size);
        return PageResult.from(recordRepository.findAllByOrderByRecordTimeDesc(pageRequest).map(DtoMapper::toMaintenanceResponse));
    }

    @Transactional(readOnly = true)
    public PageResult<MaintenanceDtos.MaintenanceResponse> byCar(Long carId, int page, int size) {
        return PageResult.from(recordRepository.findByCarIdOrderByRecordTimeDesc(carId, pageRequest(page, size))
                .map(DtoMapper::toMaintenanceResponse));
    }

    public MaintenanceDtos.MaintenanceResponse update(Long id, MaintenanceDtos.MaintenanceRequest request) {
        MaintenanceRecord record = recordRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("维保记录不存在"));
        record.setType(request.type());
        record.setDescription(request.description());
        record.setCost(request.cost() == null ? BigDecimal.ZERO : request.cost());
        record.setRecordTime(request.recordTime() == null ? LocalDateTime.now() : request.recordTime());
        return DtoMapper.toMaintenanceResponse(record);
    }

    public void delete(Long id) {
        MaintenanceRecord record = recordRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("维保记录不存在"));
        recordRepository.delete(record);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
    }
}
