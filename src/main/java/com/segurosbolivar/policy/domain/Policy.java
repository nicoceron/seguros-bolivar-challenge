// Purpose of this file: Stores a policy and applies its rules for risks, renewal, and cancellation.
package com.segurosbolivar.policy.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private int initialDurationMonths;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal premium;

    @Column(length = 160)
    private String policyholderName;

    @Column(length = 160)
    private String beneficiaryName;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Risk> risks = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Empty constructor required by JPA when it reloads this saved entity. */
    protected Policy() {
    }

    /** Validates new policy data, calculates dates and premium, and starts it as active. */
    public Policy(
            PolicyType type,
            LocalDate effectiveFrom,
            int durationMonths,
            BigDecimal monthlyRent,
            String policyholderName,
            String beneficiaryName
    ) {
        if (durationMonths < 1 || durationMonths > 1200) {
            throw new IllegalArgumentException("Duration must be between 1 and 1200 months");
        }
        this.type = Objects.requireNonNull(type, "Policy type is required");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "Effective start is required");
        this.initialDurationMonths = durationMonths;
        this.effectiveTo = effectiveFrom.plusMonths(durationMonths).minusDays(1);
        this.monthlyRent = positiveMoney(monthlyRent, "Monthly rent");
        this.premium = Money.normalize(this.monthlyRent.multiply(BigDecimal.valueOf(durationMonths)));
        this.policyholderName = requireText(policyholderName, "Policyholder");
        this.beneficiaryName = requireText(beneficiaryName, "Beneficiary");
        this.status = PolicyStatus.ACTIVA;
    }

    /** Attaches a risk; an individual policy cannot have more than one. */
    public void addRisk(Risk risk) {
        ensureNotCancelled("Cannot add a risk to a cancelled policy");
        if (type == PolicyType.INDIVIDUAL && !risks.isEmpty()) {
            throw new DomainRuleViolationException(
                    "INDIVIDUAL_RISK_LIMIT",
                    "An individual policy can have only one risk"
            );
        }
        Risk attachedRisk = Objects.requireNonNull(risk, "Risk is required");
        attachedRisk.attachTo(this);
        risks.add(attachedRisk);
    }

    /** Rejects a cancelled policy, then calculates next-term rent, premium, and dates. */
    public void renew(BigDecimal ipcPercentage) {
        ensureNotCancelled("A cancelled policy cannot be renewed");
        if (ipcPercentage == null || ipcPercentage.signum() < 0
                || ipcPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new DomainRuleViolationException(
                    "INVALID_IPC",
                    "IPC percentage must be between zero and 100"
            );
        }
        // Round rent once, then derive the premium: this preserves premium = rent * months.
        BigDecimal renewedRent = Money.increaseByPercentage(monthlyRent, ipcPercentage);
        BigDecimal renewedPremium = Money.normalize(
                renewedRent.multiply(BigDecimal.valueOf(initialDurationMonths)));
        LocalDate renewedFrom = effectiveTo.plusDays(1);
        LocalDate renewedTo = renewedFrom.plusMonths(initialDurationMonths).minusDays(1);
        monthlyRent = renewedRent;
        premium = renewedPremium;
        effectiveFrom = renewedFrom;
        effectiveTo = renewedTo;
        status = PolicyStatus.RENOVADA;
    }

    /** Marks the policy and every attached risk as cancelled. */
    public void cancel() {
        status = PolicyStatus.CANCELADA;
        risks.forEach(Risk::cancel);
    }

    /** Stops operations that are forbidden after cancellation. */
    private void ensureNotCancelled(String message) {
        if (status == PolicyStatus.CANCELADA) {
            throw new DomainRuleViolationException("POLICY_CANCELLED", message);
        }
    }

    @PrePersist
    /** JPA fills creation and update times before the first database insert. */
    void created() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /** JPA refreshes the update time before a database update. */
    void updated() {
        updatedAt = Instant.now();
    }

    /** Checks that the rounded monthly rent is positive. */
    private static BigDecimal positiveMoney(BigDecimal value, String field) {
        BigDecimal normalized = Money.normalize(value);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return normalized;
    }

    /** Requires a nonblank name and removes surrounding spaces. */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /** Returns the unique record ID. */
    public Long getId() {
        return id;
    }

    /** Returns whether the policy is individual or collective. */
    public PolicyType getType() {
        return type;
    }

    /** Returns the current record state. */
    public PolicyStatus getStatus() {
        return status;
    }

    /** Returns the first covered day. */
    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    /** Returns the last covered day. */
    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    /** Returns the original number of months, also used at renewal. */
    public int getInitialDurationMonths() {
        return initialDurationMonths;
    }

    /** Returns the monthly rent rounded to cents. */
    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    /** Returns the premium calculated from monthly rent times months. */
    public BigDecimal getPremium() {
        return premium;
    }

    /** Returns the policyholder name. */
    public String getPolicyholderName() {
        return policyholderName;
    }

    /** Returns the beneficiary name. */
    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    /** Returns risks without letting callers modify the list directly. */
    public List<Risk> getRisks() {
        return Collections.unmodifiableList(risks);
    }

    /** Returns the JPA version used to detect stale writes. */
    public long getVersion() {
        return version;
    }

    /** Returns when the row was created. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Returns when the row was last updated. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

