package com.example.demo.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RevisionFindingDeltaTest {

    @Test
    void comparesResolvedRemainingAndIntroducedCodes() {
        RevisionFindingDelta delta = RevisionFindingDelta.compare(
                1, "COPYWRITER", List.of("F01", "F02"), List.of("F02", "F03"));

        assertThat(delta.resolvedFindingCodes()).containsExactly("F01");
        assertThat(delta.remainingFindingCodes()).containsExactly("F02");
        assertThat(delta.introducedFindingCodes()).containsExactly("F03");
    }
}
