package com.example.demo.agent.visual;

public record VisualGenerationResult(
    String candidateId,
    String imageBase64,
    String mimeType,
    String model,
    VisualFormat format
) {
}