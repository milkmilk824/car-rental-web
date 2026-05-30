package com.example.carrental.repository;

import com.example.carrental.domain.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByCarIdOrderByRecordTimeDesc(Long carId);

    List<MaintenanceRecord> findAllByOrderByRecordTimeDesc();
}
