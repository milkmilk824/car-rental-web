package com.example.carrental.service;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.CommentStatus;
import com.example.carrental.common.Enums.OrderStatus;
import com.example.carrental.domain.Comment;
import com.example.carrental.domain.RentalOrder;
import com.example.carrental.dto.CommentDtos;
import com.example.carrental.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final OrderService orderService;

    public CommentService(CommentRepository commentRepository, OrderService orderService) {
        this.commentRepository = commentRepository;
        this.orderService = orderService;
    }

    public CommentDtos.CommentResponse create(Long userId, CommentDtos.CommentRequest request) {
        RentalOrder order = orderService.findById(request.orderId());
        if (!order.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("不能评价他人订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw BusinessException.badRequest("订单完成后才能评价");
        }
        if (commentRepository.existsByRentalOrderIdAndUserIdAndStatusNot(order.getId(), userId, CommentStatus.REMOVED)) {
            throw BusinessException.badRequest("该订单已评价");
        }
        Comment comment = new Comment();
        comment.setUser(order.getUser());
        comment.setCar(order.getCar());
        comment.setRentalOrder(order);
        comment.setScore(request.score());
        comment.setContent(request.content());
        comment.setStatus(CommentStatus.APPROVED);
        commentRepository.save(comment);
        return DtoMapper.toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDtos.CommentResponse> byCar(Long carId) {
        return commentRepository.findByCarIdAndStatusOrderByCreateTimeDesc(carId, CommentStatus.APPROVED)
                .stream().map(DtoMapper::toCommentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CommentDtos.CommentResponse> listAll() {
        return commentRepository.findAll().stream().map(DtoMapper::toCommentResponse).toList();
    }

    public void remove(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> BusinessException.notFound("评价不存在"));
        comment.setStatus(CommentStatus.REMOVED);
    }
}
