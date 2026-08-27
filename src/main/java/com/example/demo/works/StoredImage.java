package com.example.demo.works;

public record StoredImage(String candidateId, String storageKey, String contentType,
        int width, int height, String model, String format) {}
