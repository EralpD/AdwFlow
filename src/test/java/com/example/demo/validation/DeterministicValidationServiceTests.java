package com.example.demo.validation;

import com.example.demo.agent.copywriter.AdCandidate;
import org.junit.jupiter.api.Test;
import io.micrometer.observation.ObservationRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicValidationServiceTests {

    private final DeterministicValidationService service =
            new DeterministicValidationService(
                    List.of(
                            new RequiredFieldsValidationRule(),
                            new ContentLengthValidationRule(),
                            new ForbiddenTermsValidationRule(),
                            new PlatformStructureValidationRule()
                    ),
                    ObservationRegistry.NOOP
            );

    @Test
    void acceptsACompleteLowRiskCandidate() {
        AdCandidate candidate = new AdCandidate(
                "CANDIDATE_A",
                "ANGLE_A",
                "Bring calm to your daily priorities",
                "Group tasks by project and build a clearer "
                        + "daily priority list.",
                "Start Free Trial",
                List.of("#productivity"),
                List.of()
        );

        DeterministicValidationResult result =
                service.validate(
                        List.of(candidate),
                        "Instagram"
                );

        assertTrue(result.allValid());
        assertTrue(
                result.candidateResults()
                        .getFirst()
                        .issues()
                        .isEmpty()
        );
    }

    @Test
    void createsTargetedRevisionInstructionsForBlockedClaims() {
        AdCandidate candidate = new AdCandidate(
                "CANDIDATE_A",
                "ANGLE_A",
                "100% guaranteed productivity",
                "Double your productivity with limited seats.",
                "Act Immediately",
                List.of(),
                List.of()
        );

        DeterministicValidationResult result =
                service.validate(
                        List.of(candidate),
                        "Instagram"
                );

        assertFalse(result.allValid());
        assertEquals(
                1,
                service.revisionInstructions(result).size()
        );
        assertEquals(
                "CANDIDATE_A",
                service.revisionInstructions(result)
                        .getFirst()
                        .candidateId()
        );
        assertFalse(
                service.revisionInstructions(result)
                        .getFirst()
                        .requiredChanges()
                        .isEmpty()
        );
    }
}
