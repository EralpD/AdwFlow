package com.example.demo.workflow.context;

import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Component
public final class TrustedContextPromptFormatter {

    public String format(TrustedGenerationContext context) {
        if (context == null) return "No verified context was supplied.";

        ProductCatalogData product = context.product();
        CampaignTermsData campaign = context.campaign();
        StringBuilder output = new StringBuilder();

        output.append("VERIFIED PRODUCT CATALOG\n")
                .append("Product ID: ").append(product.productId()).append('\n')
                .append("Name: ").append(product.name()).append('\n')
                .append("Capacity: ").append(product.capacityMl()).append(" ml\n")
                .append("Features: ").append(String.join("; ", product.features())).append('\n')
                .append("Cold retention: ").append(format(product.claims().coldRetentionHours())).append('\n')
                .append("Hot retention: ").append(format(product.claims().hotRetentionHours())).append('\n')
                .append("BPA-free: ").append(format(product.claims().bpaFree())).append('\n')
                .append("Verified evidence IDs: ").append(String.join(", ", context.verifiedEvidenceIds()))
                .append("\n\nVERIFIED CAMPAIGN TERMS\n")
                .append("Campaign: ").append(campaign.campaignName()).append('\n')
                .append("Objective: ").append(campaign.objective()).append('\n')
                .append("Validity: ").append(campaign.startsOn()).append(" through ").append(campaign.endsOn()).append('\n')
                .append("Offer: ").append(campaign.offerDescription()).append('\n')
                .append("Price: ").append(campaign.originalPrice()).append(' ').append(campaign.currency())
                .append("; promotional price: ").append(campaign.promotionalPrice()).append(' ').append(campaign.currency())
                .append("; discount: ").append(campaign.discountPercent()).append("%\n")
                .append("Free-shipping regions: ").append(join(campaign.freeShippingRegions())).append('\n')
                .append("Terms URL: ").append(campaign.termsUrl());

        return output.toString();
    }

    private String format(VerifiedIntegerClaim claim) {
        return claim.value() + " hours; verified=" + claim.verified() + "; evidence=" + claim.evidenceId();
    }

    private String format(VerifiedBooleanClaim claim) {
        return claim.value() + "; verified=" + claim.verified() + "; evidence=" + claim.evidenceId();
    }

    private String join(Iterable<String> values) {
        StringJoiner joiner = new StringJoiner(", ");
        values.forEach(joiner::add);
        return joiner.toString();
    }
}
