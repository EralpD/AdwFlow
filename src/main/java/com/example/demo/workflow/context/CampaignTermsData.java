package com.example.demo.workflow.context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CampaignTermsData(
        String campaignName,
        String objective,
        LocalDate startsOn,
        LocalDate endsOn,
        String offerDescription,
        String currency,
        BigDecimal originalPrice,
        BigDecimal promotionalPrice,
        Integer discountPercent,
        List<String> freeShippingRegions,
        String termsUrl
) {
    public CampaignTermsData {
        freeShippingRegions = freeShippingRegions == null
                ? List.of()
                : List.copyOf(freeShippingRegions);
    }
}
