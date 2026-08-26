package com.example.demo.agent.visual;

// General checkup for Request

import java.util.List;
import java.util.Objects;

public record VisualGenerationRequest(
    String candidateId,
    String sourceAngleId,
    String brandName,
    String headline,
    String primaryText,
    String callToAction,
    List<String> hashtags,
    VisualFormat format
) {

    public VisualGenerationRequest {
        candidateId = requireText(candidateId, "candidateId");
        sourceAngleId = requireText(sourceAngleId, "sourceAngleId");
        brandName = requireText(brandName, "brandName");
        headline = requireText(headline, "headline");
        primaryText = requireText(primaryText, "primaryText");
        callToAction = requireText(callToAction, "callToAction");

        hashtags = hashtags == null ? List.of() : List.copyOf(hashtags);
        format = Objects.requireNonNull(format, "format is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}