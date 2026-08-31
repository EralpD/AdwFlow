package com.example.demo.agent.visual.internal;

import org.springframework.stereotype.Component;

import com.example.demo.agent.visual.VisualGenerationRequest;

@Component
public final class AdvertisementVisualPromptFactory {

    public String create(VisualGenerationRequest request) {
        String hashtags = request.hashtags().isEmpty()
            ? "None"
            : String.join(", ", request.hashtags());

        return """
            Create a single, polished commercial advertising image for a social
            media campaign.

            ADVERTISEMENT OBJECTIVE
            Build an aspirational but believable lifestyle advertisement that
            visually communicates the campaign message. The result must look
            like a professionally art-directed brand campaign, not a generic
            stock photo.

            BRAND AND CAMPAIGN INFORMATION
            Brand: %s
            Campaign headline: %s
            Campaign message: %s
            Call-to-action intent: %s
            Supporting overlay text: %s
            Offer badge text: %s
            Required disclosure: %s
            Campaign themes: %s

            CREATIVE DIRECTION
            Translate the campaign message into one clear visual story.

            Candidate-specific art direction: %s

            Show a calm and premium morning environment. The main subject should
            feel naturally integrated into the scene and should immediately be
            recognizable as the focus of the advertisement.

            Use realistic commercial photography with:
            - natural-looking materials,
            - premium product styling,
            - soft directional lighting,
            - controlled shadows,
            - realistic depth of field,
            - subtle background separation,
            - refined editorial color grading.

            The image should communicate:
            - calm confidence,
            - an intentional morning routine,
            - premium but accessible quality,
            - focus and productivity,
            - a warm and inviting atmosphere.

            SCENE AND SUBJECT PLACEMENT
            Place the primary product or subject in the right-center portion of
            the composition.

            Keep the left side visually clean and relatively uncluttered so that
            the frontend can place the campaign headline and supporting text
            there.

            The main subject must remain clearly visible when the image is viewed
            on a mobile screen. Avoid placing important objects close to the
            edges.

            Create one coherent scene. Do not create a collage, split-screen,
            mood board, contact sheet, or multiple advertising alternatives.

            COLOR AND LIGHTING
            Use warm morning sunlight entering from the side.

            Preferred palette:
            - warm cream,
            - soft beige,
            - muted caramel,
            - natural wood,
            - restrained dark brown accents.

            Maintain sufficient contrast between the clean text area and the
            background so that dark HTML text can be added later.

            TEXT-SAFE AREA
            Reserve approximately 35 percent of the composition as negative
            space for frontend text overlays.

            Do not place faces, products, hands, cups, packaging, highlights,
            or visually complex objects inside the text-safe area.

            FORMAT REQUIREMENTS
            %s

            IMPORTANT TEXT RULE
            Do not render any text inside the image.

            Specifically, do not generate:
            - the campaign headline,
            - body copy,
            - call-to-action buttons,
            - brand names,
            - product labels,
            - hashtags,
            - prices,
            - discount percentages,
            - logos,
            - watermarks,
            - signatures,
            - interface elements.

            The frontend application will add all approved advertising copy after
            image generation.

            COMPLIANCE AND ACCURACY
            Do not visually imply medical, financial, health, performance, or
            scientifically proven benefits.

            Do not invent awards, certifications, statistics, endorsements,
            product features, packaging information, or promotional offers.

            Do not use false urgency, countdown timers, scarcity cues, before-and-
            after comparisons, or manipulative visual elements.

            QUALITY CHECK
            Before completing the image, ensure that:
            - there is exactly one advertisement scene,
            - the primary subject is immediately identifiable,
            - the intended text area remains clean,
            - no accidental text or logo appears,
            - the composition works on a mobile screen,
            - the image feels commercially photographed,
            - hands and human anatomy, if present, look natural,
            - objects do not merge or appear physically distorted.

            Output only the final advertising image.
            """.formatted(
                request.brandName(),
                request.headline(),
                request.primaryText(),
                request.callToAction(),
                request.supportingText(),
                request.offerBadge(),
                request.disclosureText(),
                hashtags,
                request.visualDirection(),
                request.format().compositionInstruction()
            );
    }
}
