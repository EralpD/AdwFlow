package com.example.demo.generate.api;

import com.example.demo.agent.visual.VisualGenerationResult;

public record GenerateAdvertisementVisualResponse(
    String candidateId,
    String imageBase64,
    String mimeType,
    String model,
    String format
) {

    public static GenerateAdvertisementVisualResponse from(
        VisualGenerationResult result
    ) {
        return new GenerateAdvertisementVisualResponse(
            result.candidateId(),
            result.imageBase64(),
            result.mimeType(),
            result.model(),
            result.format().name().toLowerCase()
        );
    }
}