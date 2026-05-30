package com.example.carrental.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size, int totalPages) {

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages()
        );
    }
}
