package com.segurosbolivar.policy.api.dto;

import com.segurosbolivar.policy.domain.Risk;
import com.segurosbolivar.policy.domain.RiskStatus;

public record RiskResponse(
        Long id,
        Long policyId,
        String propertyAddress,
        String tenantName,
        RiskStatus status,
        long version
) {
    public static RiskResponse from(Risk risk) {
        return new RiskResponse(
                risk.getId(),
                risk.getPolicy().getId(),
                risk.getPropertyAddress(),
                risk.getTenantName(),
                risk.getStatus(),
                risk.getVersion()
        );
    }
}

