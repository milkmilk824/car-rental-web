package com.example.carrental.repository;

import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.domain.Car;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {

    @Override
    @EntityGraph(attributePaths = {"category", "store"})
    Page<Car> findAll(Specification<Car> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "store", "images"})
    Optional<Car> findById(Long id);

    long countByStatus(CarStatus status);

    @EntityGraph(attributePaths = {"category", "store", "images"})
    List<Car> findTop6ByStatusOrderByCreateTimeDesc(CarStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "store", "images"})
    @Query("select c from Car c where c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);
}
