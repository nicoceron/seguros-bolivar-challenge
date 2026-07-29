package com.segurosbolivar.policy.integration.core;

import com.segurosbolivar.policy.integration.outbox.CoreOutboxEvent;
import com.segurosbolivar.policy.repository.CoreOutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CoreOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(CoreOutboxProcessor.class);

    private final CoreOutboxEventRepository repository;
    private final CoreEventPublisher publisher;
    private final int maxAttempts;
    private final Counter sentCounter;
    private final Counter failedCounter;

    public CoreOutboxProcessor(
            CoreOutboxEventRepository repository,
            CoreEventPublisher publisher,
            MeterRegistry meterRegistry,
            @Value("${app.core.max-attempts}") int maxAttempts
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.maxAttempts = maxAttempts;
        this.sentCounter = meterRegistry.counter("policy.core.events", "outcome", "sent");
        this.failedCounter = meterRegistry.counter("policy.core.events", "outcome", "failed");
    }

    @Transactional
    public void process(UUID eventId) {
        CoreOutboxEvent event = repository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() != com.segurosbolivar.policy.integration.outbox.OutboxStatus.PENDING) {
            return;
        }
        try {
            publisher.publish(new CoreEventPayload(
                    event.getEventType(),
                    event.getPolicyId(),
                    event.getId()
            ));
            event.markSent();
            sentCounter.increment();
        } catch (RuntimeException exception) {
            event.markFailed(exception.getMessage(), maxAttempts);
            failedCounter.increment();
            log.warn(
                    "CORE event delivery failed eventId={} policyId={} attempt={} error={}",
                    event.getId(),
                    event.getPolicyId(),
                    event.getAttempts(),
                    exception.toString()
            );
        }
    }
}
