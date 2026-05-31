package com.example.carrental.dto;

public final class UploadDtos {

    private UploadDtos() {
    }

    public record UploadResponse(String url, String filename, long size, String contentType) {
    }
}
