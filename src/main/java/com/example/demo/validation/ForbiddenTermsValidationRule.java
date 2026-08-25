package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public final class ForbiddenTermsValidationRule
        implements AdCandidateValidationRule {

    private static final Map<String, String> FORBIDDEN_TERMS =
            forbiddenTerms();

    @Override
    public List<CandidateValidationIssue> validate(
            AdCandidate candidate,
            String platform
    ) {
        String completeText = String.join(
                " ",
                safe(candidate.headline()),
                safe(candidate.primaryText()),
                safe(candidate.callToAction())
        ).toLowerCase(Locale.ROOT);

        List<CandidateValidationIssue> issues =
                new ArrayList<>();

        FORBIDDEN_TERMS.forEach((term, replacement) -> {
            if (completeText.contains(term)) {
                issues.add(
                        new CandidateValidationIssue(
                                "FORBIDDEN_TERM_"
                                        + normalizeCode(term),
                                "overall",
                                "Candidate contains a blocked phrase: "
                                        + term,
                                replacement
                        )
                );
            }
        });

        return List.copyOf(issues);
    }

    private static Map<String, String> forbiddenTerms() {
        Map<String, String> terms = new LinkedHashMap<>();

        terms.put(
                "100% guaranteed",
                "Remove the absolute guarantee and use a factual, "
                        + "supportable benefit."
        );
        terms.put(
                "double your productivity",
                "Remove the unsupported productivity multiplier."
        );
        terms.put(
                "limited seats",
                "Remove scarcity unless a real, supplied capacity "
                        + "limit exists."
        );
        terms.put(
                "act immediately",
                "Replace pressure-based urgency with a calm call "
                        + "to action."
        );
        terms.put(
                "risk-free",
                "Remove the absolute risk claim unless it is fully "
                        + "supported by supplied terms."
        );

        return Map.copyOf(terms);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeCode(String term) {
        return term
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
