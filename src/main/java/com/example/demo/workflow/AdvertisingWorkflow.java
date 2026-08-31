package com.example.demo.workflow;

import com.example.demo.agent.copywriter.*;
import com.example.demo.agent.core.*;
import com.example.demo.agent.review.*;
import com.example.demo.agent.review.decision.*;
import com.example.demo.agent.strategy.*;
import com.example.demo.validation.*;
import com.example.demo.workflow.context.*;
import com.example.demo.workflow.routing.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public final class AdvertisingWorkflow {

    private static final int MAX_REVISION_ROUNDS = 2;

    private final CreativeStrategistAgent strategistAgent;
    private final CopywriterAgent copywriterAgent;
    private final ComplianceAgent complianceAgent;
    private final AgentExecutor agentExecutor;
    private final ReviewDecisionService reviewDecisionService;
    private final DeterministicValidationService validationService;
    private final ProductCatalogTool productCatalogTool;
    private final CampaignTermsTool campaignTermsTool;
    private final ComplianceFindingRouter findingRouter;

    public AdvertisingWorkflow(
            CreativeStrategistAgent strategistAgent,
            CopywriterAgent copywriterAgent,
            ComplianceAgent complianceAgent,
            AgentExecutor agentExecutor,
            ReviewDecisionService reviewDecisionService,
            DeterministicValidationService validationService,
            ProductCatalogTool productCatalogTool,
            CampaignTermsTool campaignTermsTool,
            ComplianceFindingRouter findingRouter
    ) {
        this.strategistAgent = strategistAgent;
        this.copywriterAgent = copywriterAgent;
        this.complianceAgent = complianceAgent;
        this.agentExecutor = agentExecutor;
        this.reviewDecisionService = reviewDecisionService;
        this.validationService = validationService;
        this.productCatalogTool = productCatalogTool;
        this.campaignTermsTool = campaignTermsTool;
        this.findingRouter = findingRouter;
    }

    public AdvertisingGenerationResult generateAdvertisement(AdvertisingGenerationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String workflowId = UUID.randomUUID().toString();
        String generationId = UUID.randomUUID().toString();
        AgentContext context = AgentContext.initial(workflowId, generationId);

        TrustedToolResult<ProductCatalogData> product = productCatalogTool.resolve(command.product());
        TrustedToolResult<CampaignTermsData> campaign = campaignTermsTool.resolve(command.campaign());
        List<String> missingInputs = new ArrayList<>();
        missingInputs.addAll(product.missingInputs());
        missingInputs.addAll(campaign.missingInputs());
        missingInputs = missingInputs.stream().distinct().toList();

        if (!missingInputs.isEmpty()) {
            return result(workflowId, generationId, GenerationStatus.NEEDS_USER_INPUT, 0,
                    null, List.of(), null, null, null, null, missingInputs, List.of());
        }

        List<String> evidenceIds = new ArrayList<>(product.verifiedEvidenceIds());
        evidenceIds.addAll(campaign.verifiedEvidenceIds());
        TrustedGenerationContext trustedContext = new TrustedGenerationContext(
                product.data(), campaign.data(), evidenceIds.stream().distinct().toList());

        StrategyResult strategy = createStrategy(
                command.toStrategyRequest(trustedContext),
                context.withAttribute("stage", "strategy"));
        List<AdCandidate> candidates = createCandidates(
                strategy, command.platform(), command.language(), trustedContext,
                context.withAttribute("stage", "copywriter")).candidates();

        int revisionRounds = 0;
        List<RevisionFindingDelta> findingDeltas = new ArrayList<>();
        List<String> pendingFindingCodes = null;
        String pendingRoute = null;
        int pendingRound = 0;

        while (true) {
            DeterministicValidationResult validation = validationService.validate(candidates, command.platform());
            if (!validation.allValid()) {
                if (revisionRounds >= MAX_REVISION_ROUNDS) {
                    return result(workflowId, generationId, GenerationStatus.REVISION_LIMIT_REACHED,
                            revisionRounds, strategy, candidates, validation, null, null,
                            trustedContext, List.of(), findingDeltas);
                }
                List<AdCandidate> invalid = validationService.invalidCandidates(candidates, validation);
                CopywriterResult revised = reviseCandidates(
                        strategy, command.platform(), command.language(), invalid,
                        validationService.revisionInstructions(validation), trustedContext,
                        context.withAttribute("stage", "deterministic-revision"));
                candidates = mergeCandidates(candidates, revised.candidates());
                revisionRounds++;
                continue;
            }

            ReviewResult review = reviewCandidates(strategy, candidates, command.platform(),
                    command.reviewLanguage(), trustedContext,
                    context.withAttribute("stage", "compliance"));
            List<String> currentFindingCodes = findingCodes(review);
            if (pendingFindingCodes != null) {
                findingDeltas.add(RevisionFindingDelta.compare(
                        pendingRound, pendingRoute, pendingFindingCodes, currentFindingCodes));
                pendingFindingCodes = null;
            }

            ReviewDecisionResult decision = decide(candidates, review);
            if (decision.decision() == ReviewDecision.PASS) {
                return result(workflowId, generationId, GenerationStatus.PASS, revisionRounds,
                        strategy, candidates, validation, review, decision,
                        trustedContext, List.of(), findingDeltas);
            }

            FindingRoutingResult routing = findingRouter.route(review);
            if (routing.route() == FindingRoute.USER_INPUT) {
                return result(workflowId, generationId, GenerationStatus.NEEDS_USER_INPUT,
                        revisionRounds, strategy, candidates, validation, review, decision,
                        trustedContext, routing.missingInputs(), findingDeltas);
            }

            if (revisionRounds >= MAX_REVISION_ROUNDS) {
                return result(workflowId, generationId, GenerationStatus.REVISION_LIMIT_REACHED,
                        revisionRounds, strategy, candidates, validation, review, decision,
                        trustedContext, List.of(), findingDeltas);
            }

            pendingFindingCodes = currentFindingCodes;
            pendingRoute = routing.route().name();
            pendingRound = revisionRounds + 1;

            if (routing.route() == FindingRoute.STRATEGIST) {
                strategy = createStrategy(
                        command.toStrategyRequest(trustedContext, routing.strategyGuidance()),
                        context.withAttribute("stage", "strategy-revision"));
                candidates = createCandidates(strategy, command.platform(), command.language(),
                        trustedContext, context.withAttribute("stage", "copywriter-after-strategy-revision"))
                        .candidates();
            } else {
                CopywriterResult revised = reviseCandidates(
                        strategy, command.platform(), command.language(), decision.candidatesToRevise(),
                        decision.revisionInstructions(), trustedContext,
                        context.withAttribute("stage", "compliance-revision"));
                candidates = mergeCandidates(candidates, revised.candidates());
            }
            revisionRounds++;
        }
    }

    public StrategyResult createStrategy(StrategyRequest request, AgentContext context) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        return agentExecutor.execute(strategistAgent, request, context,
                AgentExecutionPolicy.noRetry()).output();
    }

    public CopywriterResult createCandidates(StrategyResult strategy, String platform,
            String language, TrustedGenerationContext trustedContext, AgentContext context) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(context, "context must not be null");
        GenerateCopyRequest request = new GenerateCopyRequest(
                strategy, platform, language, trustedContext);
        return agentExecutor.execute(copywriterAgent, request, context,
                AgentExecutionPolicy.noRetry()).output();
    }

    public ReviewResult reviewCandidates(StrategyResult strategy, List<AdCandidate> candidates,
            String platform, String reviewLanguage, TrustedGenerationContext trustedContext,
            AgentContext context) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(context, "context must not be null");
        ReviewRequest request = new ReviewRequest(
                strategy, candidates, platform, reviewLanguage, trustedContext);
        return agentExecutor.execute(complianceAgent, request, context,
                AgentExecutionPolicy.noRetry()).output();
    }

    public ReviewDecisionResult decide(List<AdCandidate> candidates, ReviewResult review) {
        return reviewDecisionService.decide(
                Objects.requireNonNull(candidates, "candidates must not be null"),
                Objects.requireNonNull(review, "review must not be null"));
    }

    public CopywriterResult reviseCandidates(StrategyResult strategy, String platform,
            String language, List<AdCandidate> candidatesToRevise,
            List<RevisionInstruction> instructions, TrustedGenerationContext trustedContext,
            AgentContext context) {
        ReviseCopyRequest request = new ReviseCopyRequest(
                Objects.requireNonNull(strategy, "strategy must not be null"),
                platform, language,
                Objects.requireNonNull(candidatesToRevise, "candidatesToRevise must not be null"),
                Objects.requireNonNull(instructions, "instructions must not be null"),
                trustedContext);
        return agentExecutor.execute(copywriterAgent, request,
                Objects.requireNonNull(context, "context must not be null"),
                AgentExecutionPolicy.noRetry()).output();
    }

    private List<String> findingCodes(ReviewResult review) {
        return review.candidateReviews().stream()
                .flatMap(candidate -> candidate.findings().stream())
                .map(ComplianceFinding::code)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private List<AdCandidate> mergeCandidates(List<AdCandidate> current, List<AdCandidate> revised) {
        Map<String, AdCandidate> revisedById = new HashMap<>();
        for (AdCandidate candidate : revised) {
            if (revisedById.put(candidate.candidateId(), candidate) != null) {
                throw new IllegalStateException("Duplicate revised candidate ID: " + candidate.candidateId());
            }
        }
        List<AdCandidate> merged = current.stream()
                .map(candidate -> revisedById.getOrDefault(candidate.candidateId(), candidate))
                .toList();
        long matched = current.stream().map(AdCandidate::candidateId).filter(revisedById::containsKey).count();
        if (matched != revisedById.size()) {
            throw new IllegalStateException("Revision returned an unknown candidate ID");
        }
        return merged;
    }

    private AdvertisingGenerationResult result(
            String workflowId, String generationId, GenerationStatus status, int revisionRounds,
            StrategyResult strategy, List<AdCandidate> candidates,
            DeterministicValidationResult validation, ReviewResult review,
            ReviewDecisionResult decision, TrustedGenerationContext trustedContext,
            List<String> missingInputs, List<RevisionFindingDelta> findingDeltas
    ) {
        return new AdvertisingGenerationResult(
                workflowId, generationId, status, revisionRounds, strategy, candidates,
                validation, review, decision, trustedContext, missingInputs, findingDeltas);
    }
}
