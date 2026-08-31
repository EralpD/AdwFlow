package com.example.demo.agent.strategy;

import com.example.demo.workflow.context.TrustedGenerationContext;

import java.util.List;

// The class which enters to Brief&Strategy Agent

public record StrategyRequest(
        String brief,
        String platform,
        String brandName,
        String brandVoice,
        String knownTargetAudience,
        String language,
        int requestedAngleCount,
        TrustedGenerationContext trustedContext,
        List<String> revisionGuidance
) {

    private static final int DEFAULT_ANGLE_COUNT = 3;
    private static final int MAX_ANGLE_COUNT = 5;

    public StrategyRequest {
        brief = requireText(brief, "brief");
        platform = defaultIfBlank(platform, "unspecified");
        brandName = defaultIfBlank(brandName, "unspecified");
        brandVoice = defaultIfBlank(brandVoice, "unspecified");
        knownTargetAudience =
                defaultIfBlank(knownTargetAudience, "unspecified");
        language = defaultIfBlank(language, "Turkish");
        revisionGuidance = revisionGuidance == null
                ? List.of()
                : List.copyOf(revisionGuidance);

        if (requestedAngleCount == 0) {
            requestedAngleCount = DEFAULT_ANGLE_COUNT;
        }

        if (requestedAngleCount < 1 || requestedAngleCount > MAX_ANGLE_COUNT) {
            throw new IllegalArgumentException(
                    "requestedAngleCount must be between 1 and "
                            + MAX_ANGLE_COUNT
            );
        }
    }

    public static StrategyRequest fromBrief(String brief) {
        return new StrategyRequest(
                brief,
                "unspecified",
                "unspecified",
                "unspecified",
                "unspecified",
                "Turkish",
                DEFAULT_ANGLE_COUNT
                , null,
                List.of()
        );
    }

    private static String requireText(String value, String fieldName) {
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
