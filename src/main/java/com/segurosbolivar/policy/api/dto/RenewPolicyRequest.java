package com.segurosbolivar.policy.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RenewPolicyRequest(
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal ipcPercentage
) {
}

