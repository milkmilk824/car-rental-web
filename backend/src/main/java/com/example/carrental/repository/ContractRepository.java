package com.example.carrental.repository;

import com.example.carrental.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByRentalOrderId(Long orderId);

    List<Contract> findAllByOrderByCreateTimeDesc();
}
