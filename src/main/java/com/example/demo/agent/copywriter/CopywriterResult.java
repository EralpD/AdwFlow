package com.example.demo.agent.copywriter;

// Resulf from Copywriter

import java.util.List;

public record CopywriterResult(
        List<AdCandidate> candidates
) {

    public CopywriterResult {
        candidates = candidates == null
                ? List.of()
                : List.copyOf(candidates);
    }
}   