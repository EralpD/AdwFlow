package com.example.demo.workflow.context;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Trusted, deterministic boundary for campaign terms and pricing. */
@Component
public final class CampaignTermsTool {

    public TrustedToolResult<CampaignTermsData> resolve(CampaignTermsData campaign) {
        List<String> missing = new ArrayList<>();
        if (campaign == null) {
            return new TrustedToolResult<>(null, List.of("campaign"), List.of());
        }

        requireText(campaign.campaignName(), "campaign.campaignName", missing);
        requireText(campaign.objective(), "campaign.objective", missing);
        requireText(campaign.offerDescription(), "campaign.offerDescription", missing);
        requireText(campaign.currency(), "campaign.currency", missing);
        requireText(campaign.termsUrl(), "campaign.termsUrl", missing);

        if (campaign.startsOn() == null) missing.add("campaign.startsOn");
        if (campaign.endsOn() == null) missing.add("campaign.endsOn");
        if (campaign.startsOn() != null && campaign.endsOn() != null
                && campaign.endsOn().isBefore(campaign.startsOn())) {
            missing.add("campaign.endsOn (must be on or after startsOn)");
        }

        validateStrictlyPositive(campaign.originalPrice(), "campaign.originalPrice", missing);
        validateNonNegative(campaign.promotionalPrice(), "campaign.promotionalPrice", missing);
        if (campaign.originalPrice() != null && campaign.promotionalPrice() != null
                && campaign.promotionalPrice().compareTo(campaign.originalPrice()) > 0) {
            missing.add("campaign.promotionalPrice (must not exceed originalPrice)");
        }
        if (campaign.discountPercent() == null
                || campaign.discountPercent() < 0
                || campaign.discountPercent() > 100) {
            missing.add("campaign.discountPercent");
        }
        if (campaign.originalPrice() != null && campaign.originalPrice().signum() > 0
                && campaign.promotionalPrice() != null && campaign.promotionalPrice().signum() >= 0
                && campaign.discountPercent() != null) {
            int calculatedDiscount = campaign.originalPrice()
                    .subtract(campaign.promotionalPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(campaign.originalPrice(), 0, RoundingMode.HALF_UP)
                    .intValue();
            if (calculatedDiscount != campaign.discountPercent()) {
                missing.add("campaign.discountPercent (does not match prices)");
            }
        }
        if (campaign.freeShippingRegions().isEmpty()) {
            missing.add("campaign.freeShippingRegions");
        }

        return new TrustedToolResult<>(campaign, missing, List.of());
    }

    private static void validateStrictlyPositive(BigDecimal value, String path, List<String> missing) {
        if (value == null || value.signum() <= 0) missing.add(path);
    }

    private static void validateNonNegative(BigDecimal value, String path, List<String> missing) {
        if (value == null || value.signum() < 0) missing.add(path);
    }

    private static void requireText(String value, String path, List<String> missing) {
        if (value == null || value.isBlank()) missing.add(path);
    }
}
