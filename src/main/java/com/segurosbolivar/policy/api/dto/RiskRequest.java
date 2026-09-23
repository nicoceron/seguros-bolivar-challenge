// Purpose of this file: Receives and validates a risk address and tenant name.
package com.segurosbolivar.policy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Input fields for a property address and tenant name. */
public record RiskRequest(
        @NotBlank @Size(max = 240) String propertyAddress,
        @NotBlank @Size(max = 160) String tenantName
) {
}

