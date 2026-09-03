package com.example.demo.workflow;

import com.example.demo.agent.strategy.StrategyRequest;
import com.example.demo.workflow.context.CampaignTermsData;
import com.example.demo.workflow.context.ProductCatalogData;
import com.example.demo.workflow.context.TrustedGenerationContext;

import java.util.List;

public record AdvertisingGenerationCommand(
        String brief,
        String platform,
        String brandName,
        String brandVoice,
        String knownTargetAudience,
        String language,
        String reviewLanguage,
        int requestedAngleCount,
        CampaignTermsData campaign,
        ProductCatalogData product
) {

    public AdvertisingGenerationCommand {
        brief = requireText(brief, "brief");
        platform = defaultIfBlank(platform, "Instagram");
        brandName = defaultIfBlank(brandName, "unspecified");
        brandVoice = defaultIfBlank(
                brandVoice,
                "Calm, clear, and motivating"
        );
        knownTargetAudience = defaultIfBlank(
                knownTargetAudience,
                "unspecified"
        );
        language = defaultIfBlank(language, "English");
        reviewLanguage = defaultIfBlank(
                reviewLanguage,
                language
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

    public StrategyRequest toStrategyRequest(TrustedGenerationContext trustedContext) {
        return toStrategyRequest(trustedContext, List.of());
    }

    public StrategyRequest toStrategyRequest(
            TrustedGenerationContext trustedContext,
            List<String> revisionGuidance
    ) {
        return new StrategyRequest(
                brief,
                platform,
                brandName,
                brandVoice,
                knownTargetAudience,
                language,
                requestedAngleCount,
                trustedContext,
                revisionGuidance
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
