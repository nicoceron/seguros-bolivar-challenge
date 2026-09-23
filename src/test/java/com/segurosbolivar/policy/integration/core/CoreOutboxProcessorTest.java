// Purpose of this file: Checks delivery states, retries, and failures.
package com.segurosbolivar.policy.integration.core;

import com.segurosbolivar.policy.integration.outbox.CoreOutboxEvent;
import com.segurosbolivar.policy.integration.outbox.OutboxStatus;
import com.segurosbolivar.policy.repository.CoreOutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class CoreOutboxProcessorTest {

    private final CoreOutboxEventRepository repository = mock(CoreOutboxEventRepository.class);
    private final CoreEventPublisher publisher = mock(CoreEventPublisher.class);
    private final CoreOutboxProcessor processor = new CoreOutboxProcessor(
            repository,
            publisher,
            new SimpleMeterRegistry(),
            5
    );

    @Test
    /** Checks successful delivery becomes SENT. */
    void marksSuccessfulDeliveryAsSent() {
        CoreOutboxEvent event = new CoreOutboxEvent(42L);
        when(repository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));

        processor.process(event.getId());

        verify(publisher).publish(any(CoreEventPayload.class));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt()).isNotNull();
    }

    @Test
    /** Checks a CORE failure schedules another attempt. */
    void schedulesRetryWhenCoreIsUnavailable() {
        CoreOutboxEvent event = new CoreOutboxEvent(42L);
        when(repository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("CORE unavailable"))
                .when(publisher)
                .publish(any(CoreEventPayload.class));

        processor.process(event.getId());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("CORE unavailable");
    }
    @Test
    /** Checks a SENT event is not delivered again. */
    void alreadySentEventsAreNotPublishedAgain() {
        CoreOutboxEvent event = new CoreOutboxEvent(42L);
        when(repository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        processor.process(event.getId());
        processor.process(event.getId());
        verify(publisher, times(1)).publish(any(CoreEventPayload.class));
    }

    @Test
    /** Checks a worker waits until the next allowed retry time. */
    void respectsBackoffEvenWhenAnotherWorkerHasAStaleBatch() {
        CoreOutboxEvent event = new CoreOutboxEvent(42L);
        event.markFailed("Retry later", 5);
        when(repository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        processor.process(event.getId());
        verifyNoInteractions(publisher);
        assertThat(event.getAttempts()).isEqualTo(1);
    }

    @Test
    /** Checks exhausted events stay FAILED for inspection. */
    void exhaustedEventsAreRetainedAsFailedWithoutFurtherDelivery() {
        CoreOutboxEvent event = new CoreOutboxEvent(42L);
        for (int i = 0; i < 5; i++) { event.markFailed("CORE unavailable", 5); }
        when(repository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        processor.process(event.getId());
        verifyNoInteractions(publisher);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(5);
    }
}
