package com.example.demo.generate.api;

import java.util.List;

import com.example.demo.agent.visual.VisualFormat;
import com.example.demo.agent.visual.VisualGenerationRequest;

public record GenerateAdvertisementVisualRequest(
    String workflowId,
    String generationId,
    String candidateId,
    String sourceAngleId,
    String brandName,
    String headline,
    String supportingText,
    String primaryText,
    String callToAction,
    String offerBadge,
    String disclosureText,
    String visualDirection,
    List<String> hashtags,
    Format format
) {

    public VisualGenerationRequest toAgentRequest() {
        if (format == null) {
            throw new IllegalArgumentException("format is required.");
        }

        return new VisualGenerationRequest(
            candidateId,
            sourceAngleId,
            brandName,
            headline,
            supportingText,
            primaryText,
            callToAction,
            offerBadge,
            disclosureText,
            visualDirection,
            hashtags,
            VisualFormat.fromClientName(format.name())
        );
    }

    public record Format(
        String name,
        int width,
        int height
    ) {
    }
}
