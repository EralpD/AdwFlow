package com.example.demo.agent.visual.internal;

// Agent process functionality definition belongs in here

import java.util.Objects;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentDescriptor;
import com.example.demo.agent.visual.VisualCreativeAgent;
import com.example.demo.agent.visual.VisualGenerationRequest;
import com.example.demo.agent.visual.VisualGenerationResult;

@Component
public final class SpringAiVisualCreativeAgent implements VisualCreativeAgent {

    private static final AgentDescriptor DESCRIPTOR = AgentDescriptor.of(
        "visual-creative",
        "1.0.0",
        "image-generation",
        "advertisement-visualization"
    );

    private final ImageModel imageModel;
    private final AdvertisementVisualPromptFactory promptFactory;
    private final String model;
    private final String quality;

    public SpringAiVisualCreativeAgent(
        ImageModel imageModel,
        AdvertisementVisualPromptFactory promptFactory,
        @Value("${ad-studio.visual.model:gpt-image-2}") String model,
        @Value("${ad-studio.visual.quality:medium}") String quality
    ) {
        this.imageModel = imageModel;
        this.promptFactory = promptFactory;
        this.model = model;
        this.quality = quality;
    }

    @Override
    public AgentDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public VisualGenerationResult execute(
        VisualGenerationRequest input,
        AgentContext context
    ) {
        Objects.requireNonNull(input, "input is required");
        Objects.requireNonNull(context, "context is required");

        String prompt = promptFactory.create(input);

        OpenAiImageOptions.Builder optionsBuilder =
            OpenAiImageOptions.builder();

        optionsBuilder.model(model);

        OpenAiImageOptions options = optionsBuilder
            .n(1)
            .quality(quality)
            .size(input.format().apiSize())
            .build();

        ImageResponse response = imageModel.call(
            new ImagePrompt(prompt, options)
        );

        if (response == null
            || response.getResult() == null
            || response.getResult().getOutput() == null) {
            throw new IllegalStateException(
                "Image model returned an empty response."
            );
        }

        String imageBase64 =
            response.getResult().getOutput().getB64Json();

        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new IllegalStateException(
                "Image model response did not contain b64_json."
            );
        }

        return new VisualGenerationResult(
            input.candidateId(),
            imageBase64,
            "image/png",
            model,
            input.format()
        );
    }
}