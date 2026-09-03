package com.example.demo.workflow.context;

import java.util.List;

public record TrustedGenerationContext(
        ProductCatalogData product,
        CampaignTermsData campaign,
        List<String> verifiedEvidenceIds,
        String campaignOwnerBrief
) {
    public TrustedGenerationContext {
        verifiedEvidenceIds = verifiedEvidenceIds == null
                ? List.of()
                : List.copyOf(verifiedEvidenceIds);
        campaignOwnerBrief = campaignOwnerBrief == null
                ? ""
                : campaignOwnerBrief.trim();
    }

    public boolean hasCampaignOwnerBrief() {
        return !campaignOwnerBrief.isBlank();
    }
}
