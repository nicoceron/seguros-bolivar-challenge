// Purpose of this file: Checks rollback and concurrent updates across policy, risks, and outbox.
package com.segurosbolivar.policy.service;

import com.segurosbolivar.policy.api.dto.RiskRequest;
import com.segurosbolivar.policy.domain.*;
import com.segurosbolivar.policy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"app.demo-data.enabled=false", "app.core.dispatch-enabled=false",
        "spring.datasource.url=jdbc:h2:mem:transactions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
class PolicyTransactionTest {
    @Autowired PolicyService service;
    @Autowired PolicyRepository policies;
    @Autowired RiskRepository risks;
    @Autowired CoreOutboxEventRepository outbox;
    @Autowired TransactionTemplate tx;
    long id;

    @BeforeEach
    /** Prepares test data or the local test server before each case. */
    void setUp() {
        tx.executeWithoutResult(status -> {
            outbox.deleteAllInBatch(); risks.deleteAllInBatch(); policies.deleteAllInBatch();
            Policy p = new Policy(PolicyType.COLECTIVA, LocalDate.of(2026, 1, 1),
                    12, new BigDecimal("100.01"), "Agency", "Owner");
            p.addRisk(new Risk("Calle 1", "Ana"));
            id = policies.saveAndFlush(p).getId();
        });
    }

    @Test
    /** Checks a failure rolls back policy, risks, and CORE event together. */
    void rollbackRestoresPolicyRisksAndOutboxTogether() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            service.cancelPolicy(id);
            throw new IllegalStateException("Abort transaction deliberately");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(service.getPolicy(id).status()).isEqualTo(PolicyStatus.ACTIVA);
        assertThat(service.getRisks(id)).allMatch(r -> r.status() == RiskStatus.ACTIVO);
        assertThat(outbox.count()).isZero();
    }

    @Test
    /** Checks simultaneous add/cancel cannot leave an invalid active risk. */
    void cancellationAndConcurrentAdditionCannotLeaveAnActiveRisk() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CyclicBarrier start = new CyclicBarrier(2);
            Future<?> cancel = pool.submit(() -> { await(start); service.cancelPolicy(id); });
            Future<?> add = pool.submit(() -> {
                await(start);
                try { service.addRisk(id, new RiskRequest("Calle 2", "Luis")); }
                catch (DomainRuleViolationException expected) {
                    assertThat(expected.getCode()).isEqualTo("POLICY_CANCELLED");
                }
            });
            cancel.get(15, TimeUnit.SECONDS);
            add.get(15, TimeUnit.SECONDS);
        }
        assertThat(service.getPolicy(id).status()).isEqualTo(PolicyStatus.CANCELADA);
        assertThat(service.getRisks(id)).allMatch(r -> r.status() == RiskStatus.CANCELADO);
        assertThat(outbox.count()).isBetween(1L, 2L);
    }

    @Test
    /** Checks the response carries the saved version. */
    void committedResponsesContainTheCurrentVersion() {
        long before = service.getPolicy(id).version();
        var renewed = service.renew(id, new BigDecimal("5.2"));
        assertThat(renewed.version()).isGreaterThan(before);
        assertThat(service.getPolicy(id).version()).isEqualTo(renewed.version());
        assertThat(renewed.premium()).isEqualByComparingTo("1262.52");
        assertThat(outbox.count()).isEqualTo(1);
    }

    /** Waits until the two concurrent operations reach the same point. */
    private static void await(CyclicBarrier barrier) {
        try { barrier.await(5, TimeUnit.SECONDS); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
