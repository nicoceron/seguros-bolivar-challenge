package com.segurosbolivar.policy.api.dto;

import com.segurosbolivar.policy.domain.Policy;
import com.segurosbolivar.policy.domain.PolicyStatus;
import com.segurosbolivar.policy.domain.PolicyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyResponse(
        Long id,
        PolicyType type,
        PolicyStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        int initialDurationMonths,
        BigDecimal monthlyRent,
        BigDecimal premium,
        String policyholderName,
        String beneficiaryName,
        int riskCount,
        long version
) {
    public static PolicyResponse from(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getType(),
                policy.getStatus(),
                policy.getEffectiveFrom(),
                policy.getEffectiveTo(),
                policy.getInitialDurationMonths(),
                policy.getMonthlyRent(),
                policy.getPremium(),
                policy.getPolicyholderName(),
                policy.getBeneficiaryName(),
                policy.getRisks().size(),
                policy.getVersion()
        );
    }
}

