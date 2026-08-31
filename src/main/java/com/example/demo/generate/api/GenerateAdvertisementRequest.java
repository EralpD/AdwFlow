package com.example.demo.generate.api;

import com.example.demo.workflow.AdvertisingGenerationCommand;
import com.example.demo.workflow.context.CampaignTermsData;
import com.example.demo.workflow.context.ProductCatalogData;

public record GenerateAdvertisementRequest(
        String brief,
        String platform,
        String brandName,
        String brandVoice,
        String knownTargetAudience,
        String language,
        String reviewLanguage,
        Integer requestedAngleCount,
        CampaignTermsData campaign,
        ProductCatalogData product
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
                        : requestedAngleCount,
                campaign,
                product
        );
    }
}
