package com.example.demo.agent.strategy;

// The representetive class of 3 angle (for samples, angle A, B and C)

import java.util.List;

public record CreativeAngle(
        String id,
        String title,
        String premise,
        List<String> keyMessages,
        PersuasionBlueprint persuasionBlueprint
) {

    public CreativeAngle {
        keyMessages = keyMessages == null
                ? List.of()
                : List.copyOf(keyMessages);
    }
}