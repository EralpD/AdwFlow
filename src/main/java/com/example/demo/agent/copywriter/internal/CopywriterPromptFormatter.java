package com.example.demo.agent.copywriter.internal;

// Reconstructing strategy result to more meaningful prompt for copywriter

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.copywriter.RevisionInstruction;
import com.example.demo.agent.strategy.BriefAnalysis;
import com.example.demo.agent.strategy.CreativeAngle;
import com.example.demo.agent.strategy.PersuasionBlueprint;
import com.example.demo.agent.strategy.StrategyResult;

@Component
public final class CopywriterPromptFormatter {

    public String formatStrategy(StrategyResult strategy) {
        StringBuilder output = new StringBuilder();

        output.append("Strategy summary: ")
                .append(strategy.strategySummary())
                .append('\n');

        BriefAnalysis analysis = strategy.briefAnalysis();

        output.append("Objective: ")
                .append(analysis.objective())
                .append('\n');

        output.append("Product or offer: ")
                .append(analysis.productOrOffer())
                .append('\n');

        output.append("Target audience: ")
                .append(analysis.targetAudience())
                .append('\n');

        output.append("Customer problem: ")
                .append(analysis.customerProblem())
                .append('\n');

        output.append("Key value proposition: ")
                .append(analysis.keyValueProposition())
                .append('\n');

        output.append("Desired action: ")
                .append(analysis.desiredAction())
                .append('\n');

        appendList(
                output,
                "Assumptions",
                analysis.assumptions()
        );

        appendList(
                output,
                "Missing information",
                analysis.missingInformation()
        );

        appendList(
                output,
                "Global constraints",
                strategy.globalConstraints()
        );

        for (CreativeAngle angle
                : strategy.creativeAngles()) {
            appendAngle(output, angle);
        }

        return output.toString();
    }

    public String formatCandidates(
            List<AdCandidate> candidates
    ) {
        StringBuilder output = new StringBuilder();

        for (AdCandidate candidate : candidates) {
            output.append("Candidate ID: ")
                    .append(candidate.candidateId())
                    .append('\n');

            output.append("Source angle ID: ")
                    .append(candidate.sourceAngleId())
                    .append('\n');

            output.append("Headline: ")
                    .append(candidate.headline())
                    .append('\n');

            output.append("Supporting visual text: ")
                    .append(candidate.supportingText())
                    .append('\n');

            output.append("Primary text: ")
                    .append(candidate.primaryText())
                    .append('\n');

            output.append("Call to action: ")
                    .append(candidate.callToAction())
                    .append('\n');

            output.append("Offer badge: ")
                    .append(candidate.offerBadge())
                    .append('\n');

            output.append("Disclosure text: ")
                    .append(candidate.disclosureText())
                    .append('\n');

            output.append("Visual direction: ")
                    .append(candidate.visualDirection())
                    .append('\n');

            appendList(
                    output,
                    "Hashtags",
                    candidate.hashtags()
            );

            appendList(
                    output,
                    "Claims used",
                    candidate.claimsUsed()
            );

            output.append('\n');
        }

        return output.toString();
    }

    public String formatRevisionInstructions(
            List<RevisionInstruction> instructions
    ) {
        StringBuilder output = new StringBuilder();

        for (RevisionInstruction instruction : instructions) {
            output.append("Candidate ID: ")
                    .append(instruction.candidateId())
                    .append('\n');

            appendList(
                    output,
                    "Problems",
                    instruction.problems()
            );

            appendList(
                    output,
                    "Required changes",
                    instruction.requiredChanges()
            );

            appendList(
                    output,
                    "Elements to preserve",
                    instruction.elementsToPreserve()
            );

            output.append('\n');
        }

        return output.toString();
    }

    private void appendAngle(
            StringBuilder output,
            CreativeAngle angle
    ) {
        PersuasionBlueprint persuasion =
                angle.persuasionBlueprint();

        output.append('\n');
        output.append("Creative angle ID: ")
                .append(angle.id())
                .append('\n');

        output.append("Title: ")
                .append(angle.title())
                .append('\n');

        output.append("Premise: ")
                .append(angle.premise())
                .append('\n');

        appendList(
                output,
                "Key messages",
                angle.keyMessages()
        );

        output.append("Awareness stage: ")
                .append(persuasion.awarenessStage())
                .append('\n');

        output.append("Primary motivation: ")
                .append(persuasion.primaryMotivation())
                .append('\n');

        output.append("Emotional tension: ")
                .append(persuasion.emotionalTension())
                .append('\n');

        output.append("Hook strategy: ")
                .append(persuasion.hookStrategy())
                .append('\n');

        output.append("Value framing: ")
                .append(persuasion.valueFraming())
                .append('\n');

        output.append("Proof strategy: ")
                .append(persuasion.proofStrategy())
                .append('\n');

        output.append("CTA strategy: ")
                .append(persuasion.ctaStrategy())
                .append('\n');

        appendList(
                output,
                "Audience objections",
                persuasion.objections()
        );

        appendList(
                output,
                "Prohibited tactics",
                persuasion.prohibitedTactics()
        );
    }

    private void appendList(
            StringBuilder output,
            String title,
            List<String> values
    ) {
        output.append(title).append(':').append('\n');

        if (values == null || values.isEmpty()) {
            output.append("- none").append('\n');
            return;
        }

        for (String value : values) {
            output.append("- ")
                    .append(value)
                    .append('\n');
        }
    }
}
