package com.example.demo.generate.api;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.review.ReviewResult;
import com.example.demo.agent.review.decision.ReviewDecisionResult;
import com.example.demo.agent.strategy.StrategyResult;
import com.example.demo.validation.DeterministicValidationResult;
import com.example.demo.workflow.GenerationStatus;
import com.example.demo.workflow.RevisionFindingDelta;
import com.example.demo.works.WorkGenerationOutcome;
import java.util.List;
import java.util.stream.IntStream;

public record SavedAdvertisementResponse(String workflowId, String generationId, GenerationStatus status,
        int revisionRounds, StrategyResult strategy, List<AdCandidate> candidates,
        DeterministicValidationResult deterministicValidation, ReviewResult review, ReviewDecisionResult decision,
        List<String> missingInputs, List<RevisionFindingDelta> revisionFindingDeltas,
        Long workId, String workUrl, List<Visual> visuals) {
    public record Visual(String candidateId, String imageUrl, String mimeType, int width, int height, String format) {}

    public static SavedAdvertisementResponse from(WorkGenerationOutcome outcome) {
        var result = outcome.generation();
        var work = outcome.savedWork();
        var images = work == null ? List.<com.example.demo.works.StoredImage>of() : work.getContent().images();
        String url = work == null ? null : "/dashboard/works/" + work.getId();
        return new SavedAdvertisementResponse(result.workflowId(), result.generationId(), result.status(),
                result.revisionRounds(), result.strategy(), result.candidates(), result.deterministicValidation(),
                result.review(), result.decision(), result.missingInputs(), result.revisionFindingDeltas(),
                work == null ? null : work.getId(), url, IntStream.range(0, images.size()).mapToObj(i -> {
                    var image = images.get(i);
                    return new Visual(image.candidateId(), url + "/images/" + i, image.contentType(),
                            image.width(), image.height(), image.format());
                }).toList());
    }
}
