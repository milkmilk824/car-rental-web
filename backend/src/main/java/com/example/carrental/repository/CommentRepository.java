package com.example.carrental.repository;

import com.example.carrental.common.Enums.CommentStatus;
import com.example.carrental.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByCarIdAndStatusOrderByCreateTimeDesc(Long carId, CommentStatus status);

    List<Comment> findByStatusOrderByCreateTimeDesc(CommentStatus status);
}
