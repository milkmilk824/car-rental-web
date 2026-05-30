package com.example.carrental.repository;

import com.example.carrental.domain.CarCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarCategoryRepository extends JpaRepository<CarCategory, Long> {

    Optional<CarCategory> findByCategoryName(String categoryName);
}
