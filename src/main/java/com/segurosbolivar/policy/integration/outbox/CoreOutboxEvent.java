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

    protected CoreOutboxEvent() {
    }

    public CoreOutboxEvent(Long policyId) {
        this.id = UUID.randomUUID();
        this.policyId = policyId;
        this.eventType = "ACTUALIZACION";
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    void created() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markSent() {
        status = OutboxStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
    }

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

    public UUID getId() {
        return id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getEventType() {
        return eventType;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}

