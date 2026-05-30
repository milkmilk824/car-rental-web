package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.MaintenanceType;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.MaintenanceRecord;
import com.example.carrental.dto.MaintenanceDtos;
import com.example.carrental.repository.MaintenanceRecordRepository;
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
        return DtoMapper.toMaintenanceResponse(record);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDtos.MaintenanceResponse> listAll() {
        return recordRepository.findAllByOrderByRecordTimeDesc().stream()
                .map(DtoMapper::toMaintenanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDtos.MaintenanceResponse> byCar(Long carId) {
        return recordRepository.findByCarIdOrderByRecordTimeDesc(carId).stream()
                .map(DtoMapper::toMaintenanceResponse)
                .toList();
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
}
