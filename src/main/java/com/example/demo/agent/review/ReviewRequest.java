package com.example.demo.agent.review;

// Only the deterministic-passed candidates given to the request

import java.util.List;
import java.util.Objects;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.strategy.StrategyResult;
import com.example.demo.workflow.context.TrustedGenerationContext;

public record ReviewRequest(
        StrategyResult strategy,
        List<AdCandidate> candidates,
        String platform,
        String reviewLanguage,
        TrustedGenerationContext trustedContext
) {

    public ReviewRequest {
        strategy = Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        candidates = candidates == null
                ? List.of()
                : List.copyOf(candidates);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "candidates must not be empty"
            );
        }

        platform = defaultIfBlank(
                platform,
                "unspecified"
        );

        reviewLanguage = defaultIfBlank(
                reviewLanguage,
                "Turkish"
        );
        trustedContext = Objects.requireNonNull(
                trustedContext,
                "trustedContext must not be null"
        );
    }

    private static String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }
}
