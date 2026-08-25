package com.example.demo.agent.copywriter;

// Request of revising unvalid detected ones by detection tools

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.example.demo.agent.strategy.StrategyResult;

public record ReviseCopyRequest(
        StrategyResult strategy,
        String platform,
        String language,
        List<AdCandidate> candidatesToRevise,
        List<RevisionInstruction> revisionInstructions
) implements CopywriterRequest {

    public ReviseCopyRequest {
        strategy = Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        platform = defaultIfBlank(platform, "unspecified");
        language = defaultIfBlank(language, "English");

        candidatesToRevise = immutableList(candidatesToRevise);
        revisionInstructions =
                immutableList(revisionInstructions);

        if (candidatesToRevise.isEmpty()) {
            throw new IllegalArgumentException(
                    "candidatesToRevise must not be empty"
            );
        }

        if (revisionInstructions.isEmpty()) {
            throw new IllegalArgumentException(
                    "revisionInstructions must not be empty"
            );
        }

        validateInstructionTargets(
                candidatesToRevise,
                revisionInstructions
        );
    }

    private static void validateInstructionTargets(
            List<AdCandidate> candidates,
            List<RevisionInstruction> instructions
    ) {
        Set<String> candidateIds = new HashSet<>();

        for (AdCandidate candidate : candidates) {
            if (candidate == null
                    || candidate.candidateId() == null
                    || candidate.candidateId().isBlank()) {
                throw new IllegalArgumentException(
                        "Every candidate must have an ID"
                );
            }

            if (!candidateIds.add(candidate.candidateId())) {
                throw new IllegalArgumentException(
                        "Duplicate candidate ID: "
                                + candidate.candidateId()
                );
            }
        }

        Set<String> instructionIds = new HashSet<>();

        for (RevisionInstruction instruction : instructions) {
            if (instruction == null) {
                throw new IllegalArgumentException(
                        "Revision instruction must not be null"
                );
            }

            if (!instructionIds.add(instruction.candidateId())) {
                throw new IllegalArgumentException(
                        "Duplicate revision instruction for: "
                                + instruction.candidateId()
                );
            }

            if (!candidateIds.contains(instruction.candidateId())) {
                throw new IllegalArgumentException(
                        "Revision instruction targets an unknown "
                                + "candidate: "
                                + instruction.candidateId()
                );
            }
        }

        if (!candidateIds.equals(instructionIds)) {
            throw new IllegalArgumentException(
                    "Every candidate to revise must have exactly "
                            + "one revision instruction"
            );
        }
    }

    private static String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}