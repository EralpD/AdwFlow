package com.example.demo.workflow.context;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Trusted, deterministic boundary for product-catalog facts. */
@Component
public final class ProductCatalogTool {

    public TrustedToolResult<ProductCatalogData> resolve(ProductCatalogData product) {
        List<String> missing = new ArrayList<>();
        List<String> evidenceIds = new ArrayList<>();

        if (product == null) {
            return new TrustedToolResult<>(null, List.of(), List.of());
        }

        requireText(product.productId(), "product.productId", missing);
        requireText(product.name(), "product.name", missing);
        if (product.capacityMl() == null || product.capacityMl() <= 0) {
            missing.add("product.capacityMl");
        }
        if (product.features().isEmpty()
                || product.features().stream().anyMatch(ProductCatalogTool::blank)) {
            missing.add("product.features");
        }

        ProductClaims claims = product.claims();
        if (claims == null) {
            missing.add("product.claims");
        } else {
            validateIntegerClaim(claims.coldRetentionHours(),
                    "product.claims.coldRetentionHours", missing, evidenceIds);
            validateIntegerClaim(claims.hotRetentionHours(),
                    "product.claims.hotRetentionHours", missing, evidenceIds);
            validateBooleanClaim(claims.bpaFree(),
                    "product.claims.bpaFree", missing, evidenceIds);
        }

        return new TrustedToolResult<>(product, missing, evidenceIds);
    }

    private static void validateIntegerClaim(VerifiedIntegerClaim claim, String path,
            List<String> missing, List<String> evidenceIds) {
        if (claim == null || claim.value() == null || claim.value() <= 0) {
            missing.add(path + ".value");
        }
        validateEvidence(claim == null ? null : claim.verified(),
                claim == null ? null : claim.evidenceId(), path, missing, evidenceIds);
    }

    private static void validateBooleanClaim(VerifiedBooleanClaim claim, String path,
            List<String> missing, List<String> evidenceIds) {
        if (claim == null || claim.value() == null) {
            missing.add(path + ".value");
        }
        validateEvidence(claim == null ? null : claim.verified(),
                claim == null ? null : claim.evidenceId(), path, missing, evidenceIds);
    }

    private static void validateEvidence(Boolean verified, String evidenceId, String path,
            List<String> missing, List<String> evidenceIds) {
        if (!Boolean.TRUE.equals(verified)) {
            missing.add(path + ".verified");
        }
        if (blank(evidenceId)) {
            missing.add(path + ".evidenceId");
        } else if (Boolean.TRUE.equals(verified)) {
            evidenceIds.add(evidenceId.trim());
        }
    }

    private static void requireText(String value, String path, List<String> missing) {
        if (blank(value)) {
            missing.add(path);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
