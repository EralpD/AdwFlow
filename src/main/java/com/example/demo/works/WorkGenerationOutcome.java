package com.example.demo.works;

import com.example.demo.workflow.AdvertisingGenerationResult;

import java.util.Objects;

public record WorkGenerationOutcome(
        AdvertisingGenerationResult generation,
        HistoryWork savedWork
) {
    public WorkGenerationOutcome {
        generation = Objects.requireNonNull(generation, "generation must not be null");
    }
}
