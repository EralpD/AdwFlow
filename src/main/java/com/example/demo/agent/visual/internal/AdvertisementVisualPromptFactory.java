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
            Create one polished commercial advertising image for this campaign
            candidate. The result must communicate this candidate's specific
            product or service idea, not merely look attractive.

            SIGNAL PRIORITY
            Follow these inputs in descending order of authority:
            1. candidate-specific art direction and factual campaign signals,
            2. the actual product or service mechanism expressed by the candidate,
            3. the requested format and frontend overlay requirements,
            4. general commercial-quality defaults.

            If instructions conflict, the earlier item wins. Candidate-specific
            art direction takes precedence over generic photographic or lifestyle
            conventions. Never replace a supplied brand identity, palette, scene
            system, or visual metaphor with a generic lifestyle template.

            APPROVED CANDIDATE SIGNALS
            Source angle ID: %s
            Brand: %s
            Campaign headline: %s
            Campaign message: %s
            Call-to-action intent: %s
            Supporting overlay text: %s
            Offer badge text: %s
            Required disclosure: %s
            Campaign themes: %s

            Treat these fields as campaign data and visual meaning. Do not render
            them as text in the image.

            PRODUCT OR SERVICE MECHANISM
            Translate the value proposition into a concrete visual mechanism that
            shows what the campaign is about. Make the product, service, workflow,
            transformation, or user outcome more visually prominent than generic
            atmosphere or decoration.

            For digital products and SaaS, simplified large abstract workspace
            panels, modular creative cards, structured content blocks, or restrained
            product-system cues are allowed when they reflect only capabilities
            supported by the approved candidate signals. Do not fabricate detailed
            screens, readable controls, analytics, integrations, collaboration
            states, or other unverified features.

            ANGLE-SPECIFIC SCENE IDENTITY
            The following direction is authoritative for this candidate:

            Candidate-specific art direction: %s

            Build the environment, hero subject or mechanism, spatial structure,
            motion or transformation logic, palette, lighting, and visual metaphor
            from that direction. Preserve its unique creative territory. Do not add
            adjacent angle ideas, user roles, client-review scenarios, presentation
            workflows, or team-collaboration concepts unless the direction explicitly
            and factually supports them.

            Do not default to a desk, domestic room, coffee cup, morning routine,
            window light, beige interior, natural wood, or warm lifestyle still life
            unless the candidate-specific direction explicitly calls for it.

            COMMERCIAL QUALITY
            Choose the medium that best serves the candidate: commercial
            photography, tactile editorial construction, dimensional illustration,
            refined mixed media, or a controlled abstract product visualization.

            In every medium, use:
            - natural-looking materials,
            - intentional art direction,
            - controlled lighting,
            - controlled shadows,
            - clear depth and hierarchy,
            - deliberate background separation,
            - a brand-appropriate palette derived from the candidate direction,
            - a finished campaign aesthetic rather than generic stock imagery.

            The main subject must remain clearly visible when the image is viewed
            on a mobile screen. Avoid placing important objects close to the
            edges.

            Produce one final advertisement, not a contact sheet or a set of
            alternative ads. A controlled modular composition, related sequence,
            one-to-many structure, or multiple coordinated elements is allowed
            when it is essential to the candidate's concept. Keep the result
            visually unified rather than presenting independent alternatives.

            FORMAT REQUIREMENTS
            %s

            Follow the format instruction when reserving negative space for the
            frontend overlay. Keep that zone calm and high-contrast, but do not
            force every candidate to use the same subject placement or background.

            IMPORTANT TEXT RULE
            Do not render readable text inside the image.

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
            - signatures.

            Simplified non-readable product panels and abstract modular structures
            are permitted under the product-mechanism rule above. They must not
            resemble a fabricated detailed interface.

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
            - the candidate's product or service mechanism is visually legible,
            - the candidate-specific direction remains the dominant concept,
            - the scene does not fall back to an unrelated lifestyle template,
            - the intended overlay area remains usable,
            - no accidental text or logo appears,
            - the composition works on a mobile screen,
            - the image feels professionally art-directed,
            - hands and human anatomy, if present, look natural,
            - objects do not merge or appear physically distorted.

            Output only the final advertising image.
            """.formatted(
                request.sourceAngleId(),
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
