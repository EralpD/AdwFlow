package com.example.demo.workflow.routing;

import com.example.demo.agent.review.ComplianceFinding;
import com.example.demo.agent.review.FindingCategory;
import com.example.demo.agent.review.ReviewResult;
import com.example.demo.agent.review.ReviewedField;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class ComplianceFindingRouter {

    public FindingRoutingResult route(ReviewResult review) {
        List<ComplianceFinding> blocking = review.candidateReviews().stream()
                .flatMap(candidate -> candidate.findings().stream())
                .filter(finding -> finding.severity().requiresRevision())
                .toList();

        List<ComplianceFinding> evidenceGaps = blocking.stream()
                .filter(finding -> finding.category() == FindingCategory.UNSUPPORTED_CLAIM)
                .toList();
        if (!evidenceGaps.isEmpty()) {
            return new FindingRoutingResult(
                    FindingRoute.USER_INPUT,
                    List.of(),
                    evidenceGaps.stream()
                            .map(finding -> "evidence required for " + finding.code()
                                    + ": " + finding.requiredChange())
                            .distinct()
                            .toList()
            );
        }

        List<String> strategyGuidance = blocking.stream()
                .filter(finding -> finding.category() == FindingCategory.BRAND_OR_STRATEGY_MISALIGNMENT)
                .filter(finding -> finding.field() == ReviewedField.OVERALL)
                .map(finding -> finding.code() + ": " + finding.requiredChange())
                .distinct()
                .toList();
        if (!strategyGuidance.isEmpty()) {
            return new FindingRoutingResult(FindingRoute.STRATEGIST, strategyGuidance, List.of());
        }

        return new FindingRoutingResult(FindingRoute.COPYWRITER, List.of(), List.of());
    }
}
