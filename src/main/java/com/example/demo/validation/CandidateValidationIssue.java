package com.example.demo.validation;

public record CandidateValidationIssue(
        String code,
        String field,
        String message,
        String requiredChange
) {

    public CandidateValidationIssue {
        code = requireText(code, "code");
        field = requireText(field, "field");
        message = requireText(message, "message");
        requiredChange = requireText(
                requiredChange,
                "requiredChange"
        );
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }
}
