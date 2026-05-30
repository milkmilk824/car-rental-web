package com.example.carrental.repository;

import com.example.carrental.common.Enums.StoreStatus;
import com.example.carrental.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByStatus(StoreStatus status);

    List<Store> findByCityContainingIgnoreCase(String city);
}
