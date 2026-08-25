package com.example.demo.agent.review;

// Includes all reviewing information in this record

public record ComplianceFinding(
        String code,
        FindingCategory category,
        FindingSeverity severity,
        ReviewedField field,
        String evidence,
        String explanation,
        String requiredChange
) {
}