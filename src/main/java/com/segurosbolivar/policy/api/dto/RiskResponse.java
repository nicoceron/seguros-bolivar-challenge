// Purpose of this file: Shapes the risk data returned to a client.
package com.segurosbolivar.policy.api.dto;

import com.segurosbolivar.policy.domain.Risk;
import com.segurosbolivar.policy.domain.RiskStatus;

/** Public JSON fields returned for one risk. */
public record RiskResponse(
        Long id,
        Long policyId,
        String propertyAddress,
        String tenantName,
        RiskStatus status,
        long version
) {
    /** Copies public risk fields into the JSON response. */
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

