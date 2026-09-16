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

    protected Policy() {
    }

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

    public void cancel() {
        status = PolicyStatus.CANCELADA;
        risks.forEach(Risk::cancel);
    }

    private void ensureNotCancelled(String message) {
        if (status == PolicyStatus.CANCELADA) {
            throw new DomainRuleViolationException("POLICY_CANCELLED", message);
        }
    }

    @PrePersist
    void created() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }

    private static BigDecimal positiveMoney(BigDecimal value, String field) {
        BigDecimal normalized = Money.normalize(value);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public PolicyType getType() {
        return type;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public int getInitialDurationMonths() {
        return initialDurationMonths;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public String getPolicyholderName() {
        return policyholderName;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public List<Risk> getRisks() {
        return Collections.unmodifiableList(risks);
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

