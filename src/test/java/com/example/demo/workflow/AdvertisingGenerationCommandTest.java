package com.example.demo.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdvertisingGenerationCommandTest {

    @Test
    void reviewLanguageFollowsOutputLanguageByDefault() {
        AdvertisingGenerationCommand command = new AdvertisingGenerationCommand(
                "brief", null, null, null, null, "Turkish", null, 3, null, null);

        assertThat(command.language()).isEqualTo("Turkish");
        assertThat(command.reviewLanguage()).isEqualTo("Turkish");
        assertThat(command.brandVoice()).isEqualTo("Sakin, net ve motive edici");
    }
}
