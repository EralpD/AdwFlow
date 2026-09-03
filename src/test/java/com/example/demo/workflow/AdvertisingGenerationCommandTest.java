package com.example.demo.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdvertisingGenerationCommandTest {

    @Test
    void reviewLanguageFollowsOutputLanguageByDefault() {
        AdvertisingGenerationCommand command = new AdvertisingGenerationCommand(
                "brief", null, null, null, null, null, null, 3, null, null);

        assertThat(command.language()).isEqualTo("English");
        assertThat(command.reviewLanguage()).isEqualTo("English");
        assertThat(command.brandVoice()).isEqualTo("Calm, clear, and motivating");
    }
}
