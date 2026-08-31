package com.example.demo.workflow.context;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedToolsTest {

    @Test
    void acceptsCompleteVerifiedProductCatalogRecord() {
        ProductCatalogData product = new ProductCatalogData(
                "HYDRAFLOW-750-BLUE", "HydraFlow Smart Bottle", 750,
                List.of("LED su içme hatırlatıcısı", "USB-C şarj"),
                new ProductClaims(
                        new VerifiedIntegerClaim(12, true, "LAB-882"),
                        new VerifiedIntegerClaim(8, true, "LAB-882"),
                        new VerifiedBooleanClaim(true, true, "CERT-104")));

        TrustedToolResult<ProductCatalogData> result = new ProductCatalogTool().resolve(product);

        assertThat(result.complete()).isTrue();
        assertThat(result.verifiedEvidenceIds()).containsExactly("LAB-882", "LAB-882", "CERT-104");
    }

    @Test
    void reportsUnverifiedEvidenceAsMissingUserInput() {
        ProductCatalogData product = new ProductCatalogData(
                "HYDRAFLOW-750-BLUE", "HydraFlow Smart Bottle", 750, List.of("USB-C şarj"),
                new ProductClaims(
                        new VerifiedIntegerClaim(12, false, null),
                        new VerifiedIntegerClaim(8, true, "LAB-882"),
                        new VerifiedBooleanClaim(true, true, "CERT-104")));

        TrustedToolResult<ProductCatalogData> result = new ProductCatalogTool().resolve(product);

        assertThat(result.missingInputs()).contains(
                "product.claims.coldRetentionHours.verified",
                "product.claims.coldRetentionHours.evidenceId");
    }

    @Test
    void rejectsCampaignWithInvalidDateOrPriceOrder() {
        CampaignTermsData campaign = new CampaignTermsData(
                "Lansman", "Dönüşüm", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1),
                "%15 indirim", "TRY", new BigDecimal("1000"), new BigDecimal("1200"),
                15, List.of("Türkiye"), "https://example.com/terms");

        TrustedToolResult<CampaignTermsData> result = new CampaignTermsTool().resolve(campaign);

        assertThat(result.missingInputs()).contains(
                "campaign.endsOn (must be on or after startsOn)",
                "campaign.promotionalPrice (must not exceed originalPrice)");
    }
}
