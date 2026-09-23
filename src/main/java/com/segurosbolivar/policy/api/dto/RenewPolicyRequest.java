// Purpose of this file: Receives and validates the IPC percentage for renewal.
package com.segurosbolivar.policy.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Single renewal input: the validated IPC percentage. */
public record RenewPolicyRequest(
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal ipcPercentage
) {
}

