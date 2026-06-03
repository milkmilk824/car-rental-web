package com.example.carrental.repository;

import com.example.carrental.common.Enums.CommentStatus;
import com.example.carrental.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "car", "rentalOrder"})
    Page<Comment> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "car", "rentalOrder"})
    java.util.Optional<Comment> findById(Long id);

    @EntityGraph(attributePaths = {"user", "car", "rentalOrder"})
    List<Comment> findByCarIdAndStatusOrderByCreateTimeDesc(Long carId, CommentStatus status);

    @EntityGraph(attributePaths = {"user", "car", "rentalOrder"})
    List<Comment> findByStatusOrderByCreateTimeDesc(CommentStatus status);

    boolean existsByRentalOrderIdAndUserIdAndStatusNot(Long orderId, Long userId, CommentStatus status);
}
