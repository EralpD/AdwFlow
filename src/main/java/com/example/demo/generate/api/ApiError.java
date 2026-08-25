package com.example.demo.generate.api;

import java.time.Instant;

public record ApiError(
        String code,
        String message,
        String workflowId,
        String generationId,
        Instant timestamp
) {
}
