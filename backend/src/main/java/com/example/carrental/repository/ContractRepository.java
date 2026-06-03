package com.example.carrental.repository;

import com.example.carrental.domain.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Override
    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<Contract> findById(Long id);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Optional<Contract> findByRentalOrderId(Long orderId);

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    List<Contract> findAllByOrderByCreateTimeDesc();

    @EntityGraph(attributePaths = {"rentalOrder", "user"})
    Page<Contract> findAllByOrderByCreateTimeDesc(Pageable pageable);
}
