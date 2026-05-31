package com.example.carrental.repository;

import com.example.carrental.domain.StoreStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    boolean existsByUserIdAndStoreId(Long userId, Long storeId);

    Optional<StoreStaff> findByUserIdAndStoreId(Long userId, Long storeId);

    List<StoreStaff> findByUserIdOrderByCreateTimeAsc(Long userId);

    List<StoreStaff> findByStoreIdOrderByCreateTimeAsc(Long storeId);
}
