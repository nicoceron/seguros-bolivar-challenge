package com.segurosbolivar.policy.integration.core;

import com.segurosbolivar.policy.integration.outbox.OutboxStatus;
import com.segurosbolivar.policy.repository.CoreOutboxEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "app.core.dispatch-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CoreOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CoreOutboxDispatcher.class);

    private final CoreOutboxEventRepository repository;
    private final CoreOutboxProcessor processor;

    public CoreOutboxDispatcher(
            CoreOutboxEventRepository repository,
            CoreOutboxProcessor processor
    ) {
        this.repository = repository;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${app.core.dispatch-delay-ms}")
    public void dispatchPending() {
        for (UUID eventId : pendingEventIds()) {
            try {
                processor.process(eventId);
            } catch (RuntimeException exception) {
                // One lock/commit failure must not starve the remainder of the batch.
                log.warn("CORE outbox processing failed eventId={}", eventId, exception);
            }
        }
    }

    private List<UUID> pendingEventIds() {
        return repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxStatus.PENDING,
                        Instant.now()
                )
                .stream()
                .map(event -> event.getId())
                .toList();
    }
}
