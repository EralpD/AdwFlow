package com.example.demo.workflow;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.copywriter.CopywriterAgent;
import com.example.demo.agent.copywriter.CopywriterResult;
import com.example.demo.agent.copywriter.GenerateCopyRequest;
import com.example.demo.agent.copywriter.RevisionInstruction;
import com.example.demo.agent.copywriter.ReviseCopyRequest;
import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentExecution;
import com.example.demo.agent.core.AgentExecutionPolicy;
import com.example.demo.agent.core.AgentExecutor;
import com.example.demo.agent.review.ComplianceAgent;
import com.example.demo.agent.review.ReviewRequest;
import com.example.demo.agent.review.ReviewResult;
import com.example.demo.agent.review.decision.ReviewDecision;
import com.example.demo.agent.review.decision.ReviewDecisionResult;
import com.example.demo.agent.review.decision.ReviewDecisionService;
import com.example.demo.agent.strategy.CreativeStrategistAgent;
import com.example.demo.agent.strategy.StrategyRequest;
import com.example.demo.agent.strategy.StrategyResult;
import com.example.demo.validation.DeterministicValidationResult;
import com.example.demo.validation.DeterministicValidationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public final class AdvertisingWorkflow {

    private static final int MAX_REVISION_ROUNDS = 2;

    private final CreativeStrategistAgent strategistAgent;
    private final CopywriterAgent copywriterAgent;
    private final ComplianceAgent complianceAgent;
    private final AgentExecutor agentExecutor;
    private final ReviewDecisionService reviewDecisionService;
    private final DeterministicValidationService validationService;

    public AdvertisingWorkflow(
            CreativeStrategistAgent strategistAgent,
            CopywriterAgent copywriterAgent,
            ComplianceAgent complianceAgent,
            AgentExecutor agentExecutor,
            ReviewDecisionService reviewDecisionService,
            DeterministicValidationService validationService
    ) {
        this.strategistAgent = strategistAgent;
        this.copywriterAgent = copywriterAgent;
        this.complianceAgent = complianceAgent;
        this.agentExecutor = agentExecutor;
        this.reviewDecisionService = reviewDecisionService;
        this.validationService = validationService;
    }

    public AdvertisingGenerationResult generateAdvertisement(
            AdvertisingGenerationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        String workflowId = UUID.randomUUID().toString();
        String generationId = UUID.randomUUID().toString();

        AgentContext context = AgentContext.initial(
                workflowId,
                generationId
        );

        StrategyResult strategy = createStrategy(
                command.toStrategyRequest(),
                context.withAttribute("stage", "strategy")
        );

        CopywriterResult initialCopy = createCandidates(
                strategy,
                command.platform(),
                command.language(),
                context.withAttribute("stage", "copywriter")
        );

        List<AdCandidate> candidates =
                initialCopy.candidates();

        int revisionRounds = 0;
        ReviewResult reviewResult = null;
        ReviewDecisionResult decisionResult = null;

        while (true) {
            DeterministicValidationResult validation =
                    validationService.validate(
                            candidates,
                            command.platform()
                    );

            if (!validation.allValid()) {
                if (revisionRounds >= MAX_REVISION_ROUNDS) {
                    return result(
                            workflowId,
                            generationId,
                            GenerationStatus.REVISION_LIMIT_REACHED,
                            revisionRounds,
                            strategy,
                            candidates,
                            validation,
                            null,
                            null
                    );
                }

                List<AdCandidate> invalidCandidates =
                        validationService.invalidCandidates(
                                candidates,
                                validation
                        );

                CopywriterResult revised = reviseCandidates(
                        strategy,
                        command.platform(),
                        command.language(),
                        invalidCandidates,
                        validationService.revisionInstructions(
                                validation
                        ),
                        context.withAttribute(
                                "stage",
                                "deterministic-revision"
                        )
                );

                candidates = mergeCandidates(
                        candidates,
                        revised.candidates()
                );

                revisionRounds++;
                reviewResult = null;
                decisionResult = null;
                continue;
            }

            reviewResult = reviewCandidates(
                    strategy,
                    candidates,
                    command.platform(),
                    command.reviewLanguage(),
                    context.withAttribute("stage", "compliance")
            );

            decisionResult = decide(
                    candidates,
                    reviewResult
            );

            if (decisionResult.decision()
                    == ReviewDecision.PASS) {
                return result(
                        workflowId,
                        generationId,
                        GenerationStatus.PASS,
                        revisionRounds,
                        strategy,
                        candidates,
                        validation,
                        reviewResult,
                        decisionResult
                );
            }

            if (revisionRounds >= MAX_REVISION_ROUNDS) {
                return result(
                        workflowId,
                        generationId,
                        GenerationStatus.REVISION_LIMIT_REACHED,
                        revisionRounds,
                        strategy,
                        candidates,
                        validation,
                        reviewResult,
                        decisionResult
                );
            }

            CopywriterResult revised = reviseCandidates(
                    strategy,
                    command.platform(),
                    command.language(),
                    decisionResult,
                    context.withAttribute(
                            "stage",
                            "compliance-revision"
                    )
            );

            candidates = mergeCandidates(
                    candidates,
                    revised.candidates()
            );

            revisionRounds++;
        }
    }

    /*
     * STAGE 1
     *
     * Converts the user brief into:
     * - brief analysis,
     * - creative angles,
     * - persuasion directions.
     */

    public StrategyResult createStrategy(
            StrategyRequest request,
            AgentContext context
    ) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        AgentExecution<StrategyResult> execution =
                agentExecutor.execute(
                        strategistAgent,
                        request,
                        context,
                        AgentExecutionPolicy.noRetry()
                );

        return execution.output();
    }

    /*
     * STAGE 2
     *
     * Creates one advertising candidate for every
     * creative angle produced by the Strategy Agent.
     */

    public CopywriterResult createCandidates(
            StrategyResult strategy,
            String platform,
            String language,
            AgentContext context
    ) {
        Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        GenerateCopyRequest request =
                new GenerateCopyRequest(
                        strategy,
                        platform,
                        language
                );

        AgentExecution<CopywriterResult> execution =
                agentExecutor.execute(
                        copywriterAgent,
                        request,
                        context,
                        AgentExecutionPolicy.noRetry()
                );

        return execution.output();
    }

    /*
     * STAGE 3
     *
     * Reviews only the candidates that passed the
     * deterministic validation stage.
     *
     * Length, forbidden-term and platform validators
     * must run before this method.
     */

    public ReviewResult reviewCandidates(
            StrategyResult strategy,
            List<AdCandidate> validCandidates,
            String platform,
            String reviewLanguage,
            AgentContext context
    ) {
        Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        Objects.requireNonNull(
                validCandidates,
                "validCandidates must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        ReviewRequest request =
                new ReviewRequest(
                        strategy,
                        validCandidates,
                        platform,
                        reviewLanguage
                );

        AgentExecution<ReviewResult> execution =
                agentExecutor.execute(
                        complianceAgent,
                        request,
                        context,
                        AgentExecutionPolicy.noRetry()
                );

        return execution.output();
    }

    /*
     * DETERMINISTIC DECISION
     *
     * This is not an agent call.
     *
     * WARNING only       -> PASS
     * ERROR or CRITICAL  -> REVISION
     */

    public ReviewDecisionResult decide(
            List<AdCandidate> validCandidates,
            ReviewResult reviewResult
    ) {
        Objects.requireNonNull(
                validCandidates,
                "validCandidates must not be null"
        );

        Objects.requireNonNull(
                reviewResult,
                "reviewResult must not be null"
        );

        return reviewDecisionService.decide(
                validCandidates,
                reviewResult
        );
    }

    /*
     * REVISION STAGE
     *
     * Runs the same Copywriter Agent again.
     * This does not create a fourth agent.
     *
     * Only candidates marked for revision are supplied
     * to the Copywriter.
     */

    public CopywriterResult reviseCandidates(
            StrategyResult strategy,
            String platform,
            String language,
            ReviewDecisionResult decisionResult,
            AgentContext context
    ) {
        Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        Objects.requireNonNull(
                decisionResult,
                "decisionResult must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        if (decisionResult.decision()
                != ReviewDecision.REVISION) {
            throw new IllegalArgumentException(
                    "Candidates can only be revised when the "
                            + "review decision is REVISION"
            );
        }

        if (decisionResult.candidatesToRevise().isEmpty()) {
            throw new IllegalArgumentException(
                    "A REVISION decision must contain at least "
                            + "one candidate to revise"
            );
        }

        return reviseCandidates(
                strategy,
                platform,
                language,
                decisionResult.candidatesToRevise(),
                decisionResult.revisionInstructions(),
                context
        );
    }

    public CopywriterResult reviseCandidates(
            StrategyResult strategy,
            String platform,
            String language,
            List<AdCandidate> candidatesToRevise,
            List<RevisionInstruction> revisionInstructions,
            AgentContext context
    ) {
        Objects.requireNonNull(
                strategy,
                "strategy must not be null"
        );

        Objects.requireNonNull(
                candidatesToRevise,
                "candidatesToRevise must not be null"
        );

        Objects.requireNonNull(
                revisionInstructions,
                "revisionInstructions must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        ReviseCopyRequest request =
                new ReviseCopyRequest(
                        strategy,
                        platform,
                        language,
                        candidatesToRevise,
                        revisionInstructions
                );

        AgentExecution<CopywriterResult> execution =
                agentExecutor.execute(
                        copywriterAgent,
                        request,
                        context,
                        AgentExecutionPolicy.noRetry()
                );

        return execution.output();
    }

    private List<AdCandidate> mergeCandidates(
            List<AdCandidate> currentCandidates,
            List<AdCandidate> revisedCandidates
    ) {
        Map<String, AdCandidate> revisedById =
                new HashMap<>();

        for (AdCandidate revised : revisedCandidates) {
            AdCandidate previous = revisedById.put(
                    revised.candidateId(),
                    revised
            );

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate revised candidate ID: "
                                + revised.candidateId()
                );
            }
        }

        List<AdCandidate> merged = currentCandidates.stream()
                .map(candidate -> revisedById.getOrDefault(
                        candidate.candidateId(),
                        candidate
                ))
                .toList();

        long matchedRevisionCount = currentCandidates.stream()
                .map(AdCandidate::candidateId)
                .filter(revisedById::containsKey)
                .count();

        if (matchedRevisionCount != revisedById.size()) {
            throw new IllegalStateException(
                    "Revision returned an unknown candidate ID"
            );
        }

        return merged;
    }

    private AdvertisingGenerationResult result(
            String workflowId,
            String generationId,
            GenerationStatus status,
            int revisionRounds,
            StrategyResult strategy,
            List<AdCandidate> candidates,
            DeterministicValidationResult validation,
            ReviewResult review,
            ReviewDecisionResult decision
    ) {
        return new AdvertisingGenerationResult(
                workflowId,
                generationId,
                status,
                revisionRounds,
                strategy,
                candidates,
                validation,
                review,
                decision
        );
    }
}
