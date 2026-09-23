// Purpose of this file: Reads and locks saved CORE event reminders.
package com.segurosbolivar.policy.repository;

import com.segurosbolivar.policy.integration.outbox.CoreOutboxEvent;
import com.segurosbolivar.policy.integration.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CoreOutboxEventRepository extends JpaRepository<CoreOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CoreOutboxEvent e where e.id = :id")
    /** Locks one saved event so two workers do not send it together. */
    Optional<CoreOutboxEvent> findByIdForUpdate(@Param("id") UUID id);

    /** Gets up to 50 due pending events, oldest first. */
    List<CoreOutboxEvent> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status,
            Instant now
    );
}

