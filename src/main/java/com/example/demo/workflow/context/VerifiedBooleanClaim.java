package com.example.demo.workflow.context;

public record VerifiedBooleanClaim(
        Boolean value,
        Boolean verified,
        String evidenceId
) {
}
