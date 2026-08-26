package com.example.demo.agent.visual;

// Images' format has been formatted in the enum.

import java.util.Locale;

public enum VisualFormat {

    PORTRAIT(
        "1024x1280",
        "Compose for a 4:5 portrait advertisement. Keep clean negative space "
            + "in the lower third for headline and call-to-action overlays."
    ),

    SQUARE(
        "1024x1024",
        "Compose for a square advertisement. Keep the subject visually balanced "
            + "and leave one side uncluttered for copy overlays."
    ),

    STORY(
        "1152x2048",
        "Compose for a 9:16 vertical story. Keep important subjects away from "
            + "the top and bottom interface-safe zones."
    );

    private final String apiSize;
    private final String compositionInstruction;

    VisualFormat(String apiSize, String compositionInstruction) {
        this.apiSize = apiSize;
        this.compositionInstruction = compositionInstruction;
    }

    public String apiSize() {
        return apiSize;
    }

    public String compositionInstruction() {
        return compositionInstruction;
    }

    public static VisualFormat fromClientName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Visual format is required.");
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Unsupported visual format: " + value,
                exception
            );
        }
    }
}