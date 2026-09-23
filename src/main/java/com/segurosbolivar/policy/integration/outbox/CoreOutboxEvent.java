// Purpose of this file: Stores the durable reminder to notify CORE, including its status and attempts.
package com.segurosbolivar.policy.integration.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "core_outbox_events")
public class CoreOutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant sentAt;

    @Version
    @Column(nullable = false)
    private long version;

    /** Empty constructor required by JPA when it reloads this saved entity. */
    protected CoreOutboxEvent() {
    }

    /** Creates a pending reminder with a stable ID for retries. */
    public CoreOutboxEvent(Long policyId) {
        this.id = UUID.randomUUID();
        this.policyId = policyId;
        this.eventType = "ACTUALIZACION";
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    /** JPA records when the reminder is first inserted. */
    void created() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Marks the reminder delivered and records the delivery time. */
    public void markSent() {
        status = OutboxStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
    }

    /** Counts a failure and schedules another attempt or marks FAILED. */
    public void markFailed(String error, int maxAttempts) {
        attempts++;
        lastError = error == null ? "Unknown CORE delivery error" : error.substring(0, Math.min(error.length(), 1000));
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
            return;
        }
        long backoffSeconds = Math.min(300L, 1L << Math.min(attempts, 8));
        nextAttemptAt = Instant.now().plusSeconds(backoffSeconds);
        status = OutboxStatus.PENDING;
    }

    /** Returns the unique record ID. */
    public UUID getId() {
        return id;
    }

    /** Returns the policy ID that CORE must be told about. */
    public Long getPolicyId() {
        return policyId;
    }

    /** Returns the event type sent to CORE. */
    public String getEventType() {
        return eventType;
    }

    /** Returns the current record state. */
    public OutboxStatus getStatus() {
        return status;
    }

    /** Returns how many delivery attempts have occurred. */
    public int getAttempts() {
        return attempts;
    }

    /** Returns when another delivery attempt is allowed. */
    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    /** Returns the latest delivery failure message. */
    public String getLastError() {
        return lastError;
    }

    /** Returns when the row was created. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Returns when the mock accepted the event. */
    public Instant getSentAt() {
        return sentAt;
    }
}

