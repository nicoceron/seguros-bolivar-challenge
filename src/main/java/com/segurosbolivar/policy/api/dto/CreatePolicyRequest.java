package com.segurosbolivar.policy.api.dto;

import com.segurosbolivar.policy.domain.PolicyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePolicyRequest(
        @NotNull PolicyType type,
        @NotNull LocalDate effectiveFrom,
        @Positive @Max(1200) int durationMonths,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal monthlyRent,
        @NotBlank @Size(max = 160) String policyholderName,
        @NotBlank @Size(max = 160) String beneficiaryName,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid RiskRequest> risks
) {
}

