package com.segurosbolivar.policy.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyTest {

    @Test
    void individualPolicyRejectsASecondRisk() {
        Policy policy = policy(PolicyType.INDIVIDUAL);
        policy.addRisk(new Risk("Address 1", "Tenant 1"));

        assertThatThrownBy(() -> policy.addRisk(new Risk("Address 2", "Tenant 2")))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessage("An individual policy can have only one risk");
    }

    @Test
    void renewalAppliesIpcToRentAndPremiumAndKeepsTheInitialDuration() {
        Policy policy = policy(PolicyType.INDIVIDUAL);

        policy.renew(new BigDecimal("5.00"));

        assertThat(policy.getMonthlyRent()).isEqualByComparingTo("1050000.00");
        assertThat(policy.getPremium()).isEqualByComparingTo("12600000.00");
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.RENOVADA);
        assertThat(policy.getEffectiveFrom()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(policy.getEffectiveTo()).isEqualTo(LocalDate.of(2027, 12, 31));
    }

    @Test
    void cancelledPolicyCannotBeRenewed() {
        Policy policy = policy(PolicyType.INDIVIDUAL);
        policy.cancel();

        assertThatThrownBy(() -> policy.renew(new BigDecimal("5.00")))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessage("A cancelled policy cannot be renewed");
    }

    @Test
    void cancellationCascadesToEveryRisk() {
        Policy policy = policy(PolicyType.COLECTIVA);
        policy.addRisk(new Risk("Address 1", "Tenant 1"));
        policy.addRisk(new Risk("Address 2", "Tenant 2"));

        policy.cancel();

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELADA);
        assertThat(policy.getRisks())
                .extracting(Risk::getStatus)
                .containsOnly(RiskStatus.CANCELADO);
    }

    @Test
    void roundedRentRemainsTheSingleSourceOfTruthForPremium() {
        Policy policy = new Policy(PolicyType.INDIVIDUAL, LocalDate.of(2026, 1, 1),
                12, new BigDecimal("100.01"), "Tenant", "Owner");
        policy.renew(new BigDecimal("5.2"));
        assertThat(policy.getMonthlyRent()).isEqualByComparingTo("105.21");
        assertThat(policy.getPremium()).isEqualByComparingTo("1262.52");
    }

    @Test
    void zeroIpcStillOpensTheNextTerm() {
        Policy policy = policy(PolicyType.INDIVIDUAL);
        policy.renew(BigDecimal.ZERO);
        assertThat(policy.getMonthlyRent()).isEqualByComparingTo("1000000.00");
        assertThat(policy.getEffectiveFrom()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.RENOVADA);
    }

    @Test
    void invalidIpcCannotBypassTheDomainBoundary() {
        Policy policy = policy(PolicyType.INDIVIDUAL);
        for (BigDecimal invalid : new BigDecimal[]{null, new BigDecimal("-0.01"), new BigDecimal("100.01")}) {
            assertThatThrownBy(() -> policy.renew(invalid)).isInstanceOf(DomainRuleViolationException.class);
        }
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVA);
    }

    @Test
    void renewalUsesCalendarMonthsAcrossLeapYears() {
        Policy policy = new Policy(PolicyType.INDIVIDUAL, LocalDate.of(2024, 2, 29),
                12, new BigDecimal("100.00"), "Tenant", "Owner");
        policy.renew(BigDecimal.ZERO);
        assertThat(policy.getEffectiveFrom()).isEqualTo(LocalDate.of(2025, 2, 28));
        assertThat(policy.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 2, 27));
    }

    @Test
    void monetaryOverflowIsRejectedBeforeChangingState() {
        Policy policy = new Policy(PolicyType.INDIVIDUAL, LocalDate.of(2026, 1, 1),
                1, new BigDecimal("99999999999999999.99"), "Tenant", "Owner");
        assertThatThrownBy(() -> policy.renew(new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(policy.getMonthlyRent()).isEqualByComparingTo("99999999999999999.99");
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVA);
    }

    private Policy policy(PolicyType type) {
        return new Policy(
                type,
                LocalDate.of(2026, 1, 1),
                12,
                new BigDecimal("1000000.00"),
                "Policyholder",
                "Beneficiary"
        );
    }
}

