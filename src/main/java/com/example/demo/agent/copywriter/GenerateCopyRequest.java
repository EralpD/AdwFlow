package com.example.demo.agent.copywriter;

// Record GenerateCopyRequest

import java.util.Objects;

import com.example.demo.agent.strategy.StrategyResult;
import com.example.demo.workflow.context.TrustedGenerationContext;

public record GenerateCopyRequest(
        StrategyResult strategy,
        String platform,
        String language,
        TrustedGenerationContext trustedContext
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
        language = defaultIfBlank(language, "Turkish");
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
