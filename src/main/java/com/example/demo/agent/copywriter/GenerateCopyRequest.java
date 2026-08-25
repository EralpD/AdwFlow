package com.example.demo.agent.copywriter;

// Record GenerateCopyRequest

import java.util.Objects;

import com.example.demo.agent.strategy.StrategyResult;

public record GenerateCopyRequest(
        StrategyResult strategy,
        String platform,
        String language
) implements CopywriterRequest {

    public GenerateCopyRequest {
        strategy = Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        if (strategy.creativeAngles().isEmpty()) {
            throw new IllegalArgumentException(
                    "Strategy must contain at least one creative angle"
            );
        }

        platform = defaultIfBlank(platform, "unspecified");
        language = defaultIfBlank(language, "English");
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
