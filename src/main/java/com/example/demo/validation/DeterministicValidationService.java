package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.copywriter.RevisionInstruction;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public final class DeterministicValidationService {

    private final List<AdCandidateValidationRule> rules;
    private final ObservationRegistry observationRegistry;

    public DeterministicValidationService(
            List<AdCandidateValidationRule> rules,
            ObservationRegistry observationRegistry
    ) {
        this.rules = List.copyOf(rules);
        this.observationRegistry = observationRegistry;
    }

    public DeterministicValidationResult validate(
            List<AdCandidate> candidates,
            String platform
    ) {
        Observation observation = Observation.createNotStarted(
                "advertising.validation",
                observationRegistry
        ).lowCardinalityKeyValue(
                "validation.platform",
                platformTag(platform)
        );

        observation.start();

        try (Observation.Scope ignored = observation.openScope()) {
            DeterministicValidationResult result =
                    validateCandidates(candidates, platform);

            observation.lowCardinalityKeyValue(
                    "validation.result",
                    result.allValid() ? "pass" : "fail"
            );

            return result;
        } catch (RuntimeException failure) {
            observation.error(failure);
            throw failure;
        } finally {
            observation.stop();
        }
    }

    private DeterministicValidationResult validateCandidates(
            List<AdCandidate> candidates,
            String platform
    ) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "candidates must not be empty"
            );
        }

        List<CandidateValidationResult> results =
                new ArrayList<>();

        for (AdCandidate candidate : candidates) {
            if (candidate == null) {
                throw new IllegalArgumentException(
                        "candidate must not be null"
                );
            }

            List<CandidateValidationIssue> issues =
                    rules.stream()
                            .flatMap(rule -> rule
                                    .validate(candidate, platform)
                                    .stream())
                            .toList();

            results.add(
                    new CandidateValidationResult(
                            candidate.candidateId(),
                            issues.isEmpty(),
                            issues
                    )
            );
        }

        boolean allValid = results.stream()
                .allMatch(CandidateValidationResult::valid);

        return new DeterministicValidationResult(
                allValid,
                results
        );
    }

    private String platformTag(String platform) {
        return "instagram".equalsIgnoreCase(platform)
                ? "instagram"
                : "other";
    }

    public List<AdCandidate> invalidCandidates(
            List<AdCandidate> candidates,
            DeterministicValidationResult validation
    ) {
        Set<String> invalidIds = new HashSet<>();

        validation.candidateResults().stream()
                .filter(result -> !result.valid())
                .map(CandidateValidationResult::candidateId)
                .forEach(invalidIds::add);

        return candidates.stream()
                .filter(candidate -> invalidIds.contains(
                        candidate.candidateId()
                ))
                .toList();
    }

    public List<RevisionInstruction> revisionInstructions(
            DeterministicValidationResult validation
    ) {
        return validation.candidateResults().stream()
                .filter(result -> !result.valid())
                .map(result -> new RevisionInstruction(
                        result.candidateId(),
                        result.issues().stream()
                                .map(CandidateValidationIssue::message)
                                .toList(),
                        result.issues().stream()
                                .map(CandidateValidationIssue::requiredChange)
                                .toList(),
                        List.of(
                                "Preserve candidateId and sourceAngleId.",
                                "Preserve content unrelated to these issues."
                        )
                ))
                .toList();
    }
}
