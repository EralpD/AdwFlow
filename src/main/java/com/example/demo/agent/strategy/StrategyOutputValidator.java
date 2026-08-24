package com.example.demo.agent.strategy;

import org.springframework.stereotype.Component;

/*
Validations are been processed:

1. Java type validation: Providing JSON to be converted into StrategyResult
2. Job role validation: Are the three angles been created, are the angles unique, is there any persuasion plan e.t.c. ?
 */

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public final class StrategyOutputValidator {

    public StrategyResult validate(
            StrategyRequest request,
            StrategyResult result
    ) {
        if (result == null) {
            throw new InvalidStrategyOutputException(
                    "Strategy result must not be null"
            );
        }

        validateBriefAnalysis(result.briefAnalysis());
        validateAngleCount(request, result);
        validateAngles(result);

        return result;
    }

    private void validateBriefAnalysis(BriefAnalysis analysis) {
        if (analysis == null) {
            throw new InvalidStrategyOutputException(
                    "Brief analysis must not be null"
            );
        }

        requireText(analysis.objective(), "briefAnalysis.objective");
        requireText(
                analysis.productOrOffer(),
                "briefAnalysis.productOrOffer"
        );
        requireText(
                analysis.targetAudience(),
                "briefAnalysis.targetAudience"
        );
        requireText(
                analysis.customerProblem(),
                "briefAnalysis.customerProblem"
        );
        requireText(
                analysis.keyValueProposition(),
                "briefAnalysis.keyValueProposition"
        );
        requireText(
                analysis.desiredAction(),
                "briefAnalysis.desiredAction"
        );
    }

    private void validateAngleCount(
            StrategyRequest request,
            StrategyResult result
    ) {
        int actualCount = result.creativeAngles().size();
        int expectedCount = request.requestedAngleCount();

        if (actualCount != expectedCount) {
            throw new InvalidStrategyOutputException(
                    "Expected "
                            + expectedCount
                            + " creative angles but received "
                            + actualCount
            );
        }
    }

    private void validateAngles(StrategyResult result) {
        Set<String> normalizedIds = new HashSet<>();
        Set<String> normalizedTitles = new HashSet<>();
        Set<String> normalizedPremises = new HashSet<>();

        for (int index = 0;
             index < result.creativeAngles().size();
             index++) {

            CreativeAngle angle = result.creativeAngles().get(index);

            if (angle == null) {
                throw new InvalidStrategyOutputException(
                        "Creative angle at index "
                                + index
                                + " must not be null"
                );
            }

            requireText(angle.id(), "creativeAngle.id");
            requireText(angle.title(), "creativeAngle.title");
            requireText(angle.premise(), "creativeAngle.premise");

            assertUnique(
                    normalizedIds,
                    angle.id(),
                    "creative angle id"
            );

            assertUnique(
                    normalizedTitles,
                    angle.title(),
                    "creative angle title"
            );

            assertUnique(
                    normalizedPremises,
                    angle.premise(),
                    "creative angle premise"
            );

            if (angle.keyMessages().isEmpty()) {
                throw new InvalidStrategyOutputException(
                        "Creative angle "
                                + angle.id()
                                + " must contain at least one key message"
                );
            }

            validatePersuasionBlueprint(
                    angle.id(),
                    angle.persuasionBlueprint()
            );
        }
    }

    private void validatePersuasionBlueprint(
            String angleId,
            PersuasionBlueprint blueprint
    ) {
        if (blueprint == null) {
            throw new InvalidStrategyOutputException(
                    "Creative angle "
                            + angleId
                            + " must contain a persuasion blueprint"
            );
        }

        if (blueprint.awarenessStage() == null) {
            throw new InvalidStrategyOutputException(
                    "Creative angle "
                            + angleId
                            + " must contain an awareness stage"
            );
        }

        requireText(
                blueprint.primaryMotivation(),
                angleId + ".primaryMotivation"
        );
        requireText(
                blueprint.emotionalTension(),
                angleId + ".emotionalTension"
        );
        requireText(
                blueprint.hookStrategy(),
                angleId + ".hookStrategy"
        );
        requireText(
                blueprint.valueFraming(),
                angleId + ".valueFraming"
        );
        requireText(
                blueprint.proofStrategy(),
                angleId + ".proofStrategy"
        );
        requireText(
                blueprint.ctaStrategy(),
                angleId + ".ctaStrategy"
        );
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidStrategyOutputException(
                    fieldName + " must not be blank"
            );
        }
    }

    private void assertUnique(
            Set<String> values,
            String candidate,
            String fieldName
    ) {
        String normalized = candidate
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        if (!values.add(normalized)) {
            throw new InvalidStrategyOutputException(
                    "Duplicate " + fieldName + ": " + candidate
            );
        }
    }
}