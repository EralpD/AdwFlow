package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;

import java.util.List;

public interface AdCandidateValidationRule {

    List<CandidateValidationIssue> validate(
            AdCandidate candidate,
            String platform
    );
}
