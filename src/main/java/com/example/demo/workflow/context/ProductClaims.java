package com.example.demo.workflow.context;

public record ProductClaims(
        VerifiedIntegerClaim coldRetentionHours,
        VerifiedIntegerClaim hotRetentionHours,
        VerifiedBooleanClaim bpaFree
) {
}
