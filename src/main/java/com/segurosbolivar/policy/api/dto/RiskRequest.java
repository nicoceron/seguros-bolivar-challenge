package com.segurosbolivar.policy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiskRequest(
        @NotBlank @Size(max = 240) String propertyAddress,
        @NotBlank @Size(max = 160) String tenantName
) {
}

