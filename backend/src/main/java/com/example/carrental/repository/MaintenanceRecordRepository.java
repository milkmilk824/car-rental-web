package com.example.carrental.repository;

import com.example.carrental.domain.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"car"})
    Optional<MaintenanceRecord> findById(Long id);

    @EntityGraph(attributePaths = {"car"})
    List<MaintenanceRecord> findByCarIdOrderByRecordTimeDesc(Long carId);

    @EntityGraph(attributePaths = {"car"})
    List<MaintenanceRecord> findAllByOrderByRecordTimeDesc();

    @EntityGraph(attributePaths = {"car"})
    Page<MaintenanceRecord> findByCarIdOrderByRecordTimeDesc(Long carId, Pageable pageable);

    @EntityGraph(attributePaths = {"car"})
    Page<MaintenanceRecord> findAllByOrderByRecordTimeDesc(Pageable pageable);
}
