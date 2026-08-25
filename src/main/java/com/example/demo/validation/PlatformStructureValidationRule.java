package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class PlatformStructureValidationRule
        implements AdCandidateValidationRule {

    private static final int INSTAGRAM_HASHTAG_LIMIT = 30;

    @Override
    public List<CandidateValidationIssue> validate(
            AdCandidate candidate,
            String platform
    ) {
        if (!"instagram".equalsIgnoreCase(platform)
                || candidate.hashtags().size()
                <= INSTAGRAM_HASHTAG_LIMIT) {
            return List.of();
        }

        return List.of(
                new CandidateValidationIssue(
                        "INSTAGRAM_HASHTAG_LIMIT",
                        "hashtags",
                        "Instagram candidate contains "
                                + candidate.hashtags().size()
                                + " hashtags; configured maximum is "
                                + INSTAGRAM_HASHTAG_LIMIT,
                        "Reduce hashtags to at most "
                                + INSTAGRAM_HASHTAG_LIMIT
                                + "."
                )
        );
    }
}
