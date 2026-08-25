package com.example.demo.workflow;

import com.example.demo.agent.strategy.StrategyRequest;

public record AdvertisingGenerationCommand(
        String brief,
        String platform,
        String brandName,
        String brandVoice,
        String knownTargetAudience,
        String language,
        String reviewLanguage,
        int requestedAngleCount
) {

    public AdvertisingGenerationCommand {
        brief = requireText(brief, "brief");
        platform = defaultIfBlank(platform, "Instagram");
        brandName = defaultIfBlank(brandName, "unspecified");
        brandVoice = defaultIfBlank(
                brandVoice,
                "Calm, clear and encouraging"
        );
        knownTargetAudience = defaultIfBlank(
                knownTargetAudience,
                "unspecified"
        );
        language = defaultIfBlank(language, "English");
        reviewLanguage = defaultIfBlank(
                reviewLanguage,
                "English"
        );

        if (requestedAngleCount == 0) {
            requestedAngleCount = 3;
        }

        if (requestedAngleCount < 1
                || requestedAngleCount > 5) {
            throw new IllegalArgumentException(
                    "requestedAngleCount must be between 1 and 5"
            );
        }
    }

    public StrategyRequest toStrategyRequest() {
        return new StrategyRequest(
                brief,
                platform,
                brandName,
                brandVoice,
                knownTargetAudience,
                requestedAngleCount
        );
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private static String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }
}
