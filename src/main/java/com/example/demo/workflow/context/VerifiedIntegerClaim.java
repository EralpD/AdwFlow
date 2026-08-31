package com.example.demo.workflow.context;

public record VerifiedIntegerClaim(
        Integer value,
        Boolean verified,
        String evidenceId
) {
}
