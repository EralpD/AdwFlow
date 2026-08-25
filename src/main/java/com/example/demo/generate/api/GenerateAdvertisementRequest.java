package com.example.demo.generate.api;

import com.example.demo.workflow.AdvertisingGenerationCommand;

public record GenerateAdvertisementRequest(
        String brief,
        String platform,
        String brandName,
        String brandVoice,
        String knownTargetAudience,
        String language,
        String reviewLanguage,
        Integer requestedAngleCount
) {

    public AdvertisingGenerationCommand toCommand() {
        return new AdvertisingGenerationCommand(
                brief,
                platform,
                brandName,
                brandVoice,
                knownTargetAudience,
                language,
                reviewLanguage,
                requestedAngleCount == null
                        ? 0
                        : requestedAngleCount
        );
    }
}
