package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public final class ContentLengthValidationRule
        implements AdCandidateValidationRule {

    private static final int MAX_HEADLINE_LENGTH = 60;
    private static final int MAX_PRIMARY_TEXT_LENGTH = 2_200;
    private static final int MAX_CALL_TO_ACTION_LENGTH = 40;

    @Override
    public List<CandidateValidationIssue> validate(
            AdCandidate candidate,
            String platform
    ) {
        List<CandidateValidationIssue> issues =
                new ArrayList<>();

        addLengthIssue(
                issues,
                candidate.headline(),
                "headline",
                "HEADLINE_TOO_LONG",
                MAX_HEADLINE_LENGTH
        );

        addLengthIssue(
                issues,
                candidate.primaryText(),
                "primaryText",
                "PRIMARY_TEXT_TOO_LONG",
                MAX_PRIMARY_TEXT_LENGTH
        );

        addLengthIssue(
                issues,
                candidate.callToAction(),
                "callToAction",
                "CALL_TO_ACTION_TOO_LONG",
                MAX_CALL_TO_ACTION_LENGTH
        );

        return List.copyOf(issues);
    }

    private void addLengthIssue(
            List<CandidateValidationIssue> issues,
            String value,
            String field,
            String code,
            int maximumLength
    ) {
        if (value == null || value.length() <= maximumLength) {
            return;
        }

        issues.add(
                new CandidateValidationIssue(
                        code,
                        field,
                        field
                                + " contains "
                                + value.length()
                                + " characters; maximum is "
                                + maximumLength,
                        "Shorten "
                                + field
                                + " to at most "
                                + maximumLength
                                + " characters."
                )
        );
    }
}
