package com.example.demo.agent.visual.internal;

import com.example.demo.agent.visual.VisualFormat;
import com.example.demo.agent.visual.VisualGenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdvertisementVisualPromptFactoryTest {

    private final AdvertisementVisualPromptFactory factory = new AdvertisementVisualPromptFactory();

    @Test
    void candidateDirectionOutranksGenericStyleDefaults() {
        var request = new VisualGenerationRequest(
                "CANDIDATE_B",
                "ANGLE_B",
                "AdwFlow",
                "One brief. Three directions.",
                "Turn one focused input into distinct campaign routes.",
                "Move from a single brief into three structured creative directions.",
                "Start your creative flow",
                "NONE",
                "NONE",
                "Use deep navy, vivid blue, white and controlled mint. Show one luminous input "
                        + "branching into three visually distinct modular creative systems.",
                List.of("#AdwFlow", "#CreativeWorkflow"),
                VisualFormat.PORTRAIT
        );

        String prompt = factory.create(request);
        String normalizedPrompt = prompt.replaceAll("\\s+", " ");

        assertThat(normalizedPrompt)
                .contains("Source angle ID: ANGLE_B")
                .contains("Candidate-specific art direction takes precedence")
                .contains("Use deep navy, vivid blue, white and controlled mint")
                .contains("simplified large abstract workspace panels")
                .contains("do not force every candidate to use the same subject placement")
                .doesNotContain("Show a calm and premium morning environment")
                .doesNotContain("Use warm morning sunlight entering from the side")
                .doesNotContain("Preferred palette:")
                .doesNotContain("Do not create a collage, split-screen")
                .doesNotContain("- interface elements.");
    }
}
