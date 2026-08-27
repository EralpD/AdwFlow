package com.example.demo.works;

import com.example.demo.account.UserAccountRepository;
import com.example.demo.agent.core.*;
import com.example.demo.agent.visual.*;
import com.example.demo.security.AccountPrincipal;
import com.example.demo.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class SavedWorkGenerationService {
    private static final Logger log = LoggerFactory.getLogger(SavedWorkGenerationService.class);
    private final AdvertisingWorkflow workflow;
    private final VisualCreativeAgent visuals;
    private final AgentExecutor executor;
    private final ImageStorage storage;
    private final HistoryWorkRepository history;
    private final UserAccountRepository accounts;
    private final TransactionTemplate transaction;
    private final Duration retention;

    public SavedWorkGenerationService(AdvertisingWorkflow workflow, VisualCreativeAgent visuals, AgentExecutor executor,
            ImageStorage storage, HistoryWorkRepository history, UserAccountRepository accounts,
            PlatformTransactionManager transactions, @Value("${app.works.retention:P30D}") Duration retention) {
        this.workflow = workflow;
        this.visuals = visuals;
        this.executor = executor;
        this.storage = storage;
        this.history = history;
        this.accounts = accounts;
        this.transaction = new TransactionTemplate(transactions);
        this.retention = retention;
        if (retention.isNegative() || retention.isZero()) throw new IllegalArgumentException("Work retention must be positive");
    }

    public HistoryWork generate(AccountPrincipal principal, AdvertisingGenerationCommand command) {
        requireAccount(principal);
        if (command.requestedAngleCount() != 3) throw new IllegalArgumentException("Saved works require exactly three variations.");
        AdvertisingGenerationResult result = workflow.generateAdvertisement(command);
        if (result == null || result.candidates().size() != 3
                || result.candidates().stream().anyMatch(c -> c.candidateId() == null || c.headline() == null || c.headline().isBlank())
                || result.candidates().stream().map(c -> c.candidateId()).distinct().count() != 3) {
            throw new WorkStorageException("Generation did not return three complete variations. No work was saved.");
        }
        UUID group = UUID.randomUUID();
        List<StoredImage> images = new ArrayList<>();
        AtomicInteger transactionOutcome = new AtomicInteger(TransactionSynchronization.STATUS_ROLLED_BACK);
        try {
            // No SQL transaction or connection is held while paid image requests are in progress.
            for (int i = 0; i < result.candidates().size(); i++) {
                var candidate = result.candidates().get(i);
                var input = new VisualGenerationRequest(candidate.candidateId(), candidate.sourceAngleId(),
                        command.brandName(), candidate.headline(), candidate.primaryText(), candidate.callToAction(),
                        candidate.hashtags(), VisualFormat.PORTRAIT);
                var context = AgentContext.initial(result.workflowId(), result.generationId())
                        .withAttribute("stage", "visual-generation").withAttribute("candidateId", candidate.candidateId());
                var visual = executor.execute(visuals, input, context, AgentExecutionPolicy.noRetry()).output();
                if (!candidate.candidateId().equals(visual.candidateId())) throw new WorkStorageException("Image candidate mismatch.");
                images.add(storage.store(group, i, visual));
            }
            String headline = result.candidates().getFirst().headline().strip();
            String title = headline.codePoints().limit(255).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            return Objects.requireNonNull(transaction.execute(status -> {
                transactionOutcome.set(TransactionSynchronization.STATUS_UNKNOWN);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int completionStatus) { transactionOutcome.set(completionStatus); }
                });
                requireAccount(principal);
                Instant now = Instant.now();
                return history.saveAndFlush(new HistoryWork(principal.getUserId(), title, command.brief(),
                        new WorkContent(result, images), now, now.plus(retention)));
            }));
        } catch (RuntimeException failure) {
            if (transactionOutcome.get() == TransactionSynchronization.STATUS_ROLLED_BACK) {
                try { storage.deleteGroup(group); }
                catch (RuntimeException cleanupFailure) {
                    log.error("Failed to clean incomplete image group {}", group, cleanupFailure);
                    failure.addSuppressed(cleanupFailure);
                }
            } else {
                // A lost commit acknowledgement must not delete images of a possibly committed work.
                log.error("Retained image group {} because the SQL commit outcome needs reconciliation", group);
            }
            throw failure;
        }
    }

    private void requireAccount(AccountPrincipal principal) {
        if (principal == null || principal.getUserId() == null
                || !accounts.existsByIdAndEmailAndAuthVersion(principal.getUserId(), principal.getUsername(), principal.getAuthVersion())) {
            throw new AccessDeniedException("Account is no longer available");
        }
    }
}
