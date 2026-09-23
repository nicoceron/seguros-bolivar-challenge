// Purpose of this file: Stores a covered property and tenant, and supports cancellation.
package com.segurosbolivar.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "risks")
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false, length = 240)
    private String propertyAddress;

    @Column(nullable = false, length = 160)
    private String tenantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Empty constructor required by JPA when it reloads this saved entity. */
    protected Risk() {
    }

    /** Validates the address and tenant and creates an active risk. */
    public Risk(String propertyAddress, String tenantName) {
        this.propertyAddress = requireText(propertyAddress, "Property address");
        this.tenantName = requireText(tenantName, "Tenant name");
        this.status = RiskStatus.ACTIVO;
    }

    /** Links this risk to its parent policy before saving. */
    void attachTo(Policy policy) {
        this.policy = Objects.requireNonNull(policy, "Policy is required");
    }

    /** Marks the risk cancelled without deleting its row. */
    public void cancel() {
        this.status = RiskStatus.CANCELADO;
    }

    @PrePersist
    /** JPA fills timestamps when this risk is first saved. */
    void created() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /** JPA refreshes the update time when this risk changes. */
    void updated() {
        updatedAt = Instant.now();
    }

    /** Requires nonblank text and removes surrounding spaces. */
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

    /** Returns this risk’s parent policy. */
    public Policy getPolicy() {
        return policy;
    }

    /** Returns the covered property address. */
    public String getPropertyAddress() {
        return propertyAddress;
    }

    /** Returns the covered tenant name. */
    public String getTenantName() {
        return tenantName;
    }

    /** Returns the current record state. */
    public RiskStatus getStatus() {
        return status;
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

