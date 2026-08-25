package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public final class RequiredFieldsValidationRule
        implements AdCandidateValidationRule {

    @Override
    public List<CandidateValidationIssue> validate(
            AdCandidate candidate,
            String platform
    ) {
        List<CandidateValidationIssue> issues =
                new ArrayList<>();

        addRequiredIssue(
                issues,
                candidate.headline(),
                "headline",
                "REQUIRED_HEADLINE"
        );

        addRequiredIssue(
                issues,
                candidate.primaryText(),
                "primaryText",
                "REQUIRED_PRIMARY_TEXT"
        );

        addRequiredIssue(
                issues,
                candidate.callToAction(),
                "callToAction",
                "REQUIRED_CALL_TO_ACTION"
        );

        return List.copyOf(issues);
    }

    private void addRequiredIssue(
            List<CandidateValidationIssue> issues,
            String value,
            String field,
            String code
    ) {
        if (value != null && !value.isBlank()) {
            return;
        }

        issues.add(
                new CandidateValidationIssue(
                        code,
                        field,
                        field + " is required",
                        "Provide a non-empty " + field + "."
                )
        );
    }
}
