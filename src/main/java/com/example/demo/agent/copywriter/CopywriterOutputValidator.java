package com.example.demo.agent.copywriter;

// Validation in-process (validating the structure of Copywriter output)

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.agent.strategy.CreativeAngle;

@Component
public final class CopywriterOutputValidator {

    public CopywriterResult validate(
            CopywriterRequest request,
            CopywriterResult result
    ) {
        if (result == null) {
            throw new InvalidCopywriterOutputException(
                    "Copywriter result must not be null"
            );
        }

        validateCandidateContents(result);

        switch (request) {
            case GenerateCopyRequest generateRequest ->
                    validateGeneratedCandidates(
                            generateRequest,
                            result
                    );

            case ReviseCopyRequest reviseRequest ->
                    validateRevisedCandidates(
                            reviseRequest,
                            result
                    );
        }

        return result;
    }

    private void validateCandidateContents(
            CopywriterResult result
    ) {
        if (result.candidates().isEmpty()) {
            throw new InvalidCopywriterOutputException(
                    "Copywriter result must contain candidates"
            );
        }

        Set<String> candidateIds = new HashSet<>();
        Set<String> textSignatures = new HashSet<>();

        for (AdCandidate candidate : result.candidates()) {
            if (candidate == null) {
                throw new InvalidCopywriterOutputException(
                        "Candidate must not be null"
                );
            }

            requireText(
                    candidate.candidateId(),
                    "candidateId"
            );

            requireText(
                    candidate.sourceAngleId(),
                    "sourceAngleId"
            );

            requireText(
                    candidate.headline(),
                    candidate.candidateId() + ".headline"
            );

            requireText(
                    candidate.supportingText(),
                    candidate.candidateId() + ".supportingText"
            );

            requireText(
                    candidate.primaryText(),
                    candidate.candidateId() + ".primaryText"
            );

            requireText(
                    candidate.callToAction(),
                    candidate.candidateId() + ".callToAction"
            );

            requireText(candidate.offerBadge(), candidate.candidateId() + ".offerBadge");
            requireText(candidate.disclosureText(), candidate.candidateId() + ".disclosureText");
            requireText(candidate.visualDirection(), candidate.candidateId() + ".visualDirection");

            String normalizedId =
                    normalize(candidate.candidateId());

            if (!candidateIds.add(normalizedId)) {
                throw new InvalidCopywriterOutputException(
                        "Duplicate candidate ID: "
                                + candidate.candidateId()
                );
            }

            String textSignature = normalize(
                    candidate.headline()
                            + " "
                            + candidate.primaryText()
            );

            if (!textSignatures.add(textSignature)) {
                throw new InvalidCopywriterOutputException(
                        "Duplicate advertising copy detected: "
                                + candidate.candidateId()
                );
            }
        }
    }

    private void validateGeneratedCandidates(
            GenerateCopyRequest request,
            CopywriterResult result
    ) {
        Set<String> expectedAngleIds =
                request.strategy()
                        .creativeAngles()
                        .stream()
                        .map(CreativeAngle::id)
                        .collect(Collectors.toSet());

        if (result.candidates().size()
                != expectedAngleIds.size()) {
            throw new InvalidCopywriterOutputException(
                    "Expected "
                            + expectedAngleIds.size()
                            + " candidates but received "
                            + result.candidates().size()
            );
        }

        Set<String> actualAngleIds =
                result.candidates()
                        .stream()
                        .map(AdCandidate::sourceAngleId)
                        .collect(Collectors.toSet());

        if (!actualAngleIds.equals(expectedAngleIds)) {
            throw new InvalidCopywriterOutputException(
                    "Every creative angle must produce exactly "
                            + "one advertising candidate"
            );
        }
    }

    private void validateRevisedCandidates(
            ReviseCopyRequest request,
            CopywriterResult result
    ) {
        Map<String, String> expectedCandidates =
                new HashMap<>();

        for (AdCandidate candidate
                : request.candidatesToRevise()) {
            expectedCandidates.put(
                    candidate.candidateId(),
                    candidate.sourceAngleId()
            );
        }

        if (result.candidates().size()
                != expectedCandidates.size()) {
            throw new InvalidCopywriterOutputException(
                    "Revision result must contain exactly "
                            + expectedCandidates.size()
                            + " candidates"
            );
        }

        Set<String> actualCandidateIds =
                result.candidates()
                        .stream()
                        .map(AdCandidate::candidateId)
                        .collect(Collectors.toSet());

        if (!actualCandidateIds.equals(
                expectedCandidates.keySet()
        )) {
            throw new InvalidCopywriterOutputException(
                    "Revised candidate IDs must match requested "
                            + "candidate IDs"
            );
        }

        for (AdCandidate revised : result.candidates()) {
            String expectedAngleId =
                    expectedCandidates.get(
                            revised.candidateId()
                    );

            if (!expectedAngleId.equals(
                    revised.sourceAngleId()
            )) {
                throw new InvalidCopywriterOutputException(
                        "Revision changed the source angle of "
                                + revised.candidateId()
                );
            }
        }
    }

    private void requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidCopywriterOutputException(
                    fieldName + " must not be blank"
            );
        }
    }

    private String normalize(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
