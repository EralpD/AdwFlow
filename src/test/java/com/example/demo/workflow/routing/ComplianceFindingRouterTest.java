package com.example.demo.workflow.routing;

import com.example.demo.agent.review.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceFindingRouterTest {

    private final ComplianceFindingRouter router = new ComplianceFindingRouter();

    @Test
    void routesUnsupportedClaimsToUserEvidence() {
        FindingRoutingResult result = router.route(review(new ComplianceFinding(
                "CANDIDATE_A-F01", FindingCategory.UNSUPPORTED_CLAIM, FindingSeverity.ERROR,
                ReviewedField.PRIMARY_TEXT, "24 saat", "Kanıt yok", "Laboratuvar kanıtı ekleyin")));

        assertThat(result.route()).isEqualTo(FindingRoute.USER_INPUT);
        assertThat(result.missingInputs()).singleElement().asString().contains("CANDIDATE_A-F01");
    }

    @Test
    void routesOverallStrategyMisalignmentToStrategist() {
        FindingRoutingResult result = router.route(review(new ComplianceFinding(
                "CANDIDATE_A-F02", FindingCategory.BRAND_OR_STRATEGY_MISALIGNMENT,
                FindingSeverity.ERROR, ReviewedField.OVERALL, "MISSING",
                "Açı marka vaadiyle uyuşmuyor", "Stratejik açıyı yeniden kurun")));

        assertThat(result.route()).isEqualTo(FindingRoute.STRATEGIST);
        assertThat(result.strategyGuidance()).containsExactly(
                "CANDIDATE_A-F02: Stratejik açıyı yeniden kurun");
    }

    @Test
    void routesTextAndDisclosureProblemsToCopywriter() {
        FindingRoutingResult result = router.route(review(new ComplianceFinding(
                "CANDIDATE_A-F03", FindingCategory.UNCLEAR_DISCLOSURE,
                FindingSeverity.ERROR, ReviewedField.DISCLOSURE_TEXT, "Koşullar geçerlidir",
                "Koşul belirsiz", "Tarih ve URL ekleyin")));

        assertThat(result.route()).isEqualTo(FindingRoute.COPYWRITER);
    }

    private ReviewResult review(ComplianceFinding finding) {
        return new ReviewResult("review", List.of(new CandidateReview(
                "CANDIDATE_A", "ANGLE_A", List.of(finding), List.of(), List.of(), .9)));
    }
}
