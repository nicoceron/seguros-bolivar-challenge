package com.segurosbolivar.policy.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Monetary value is required");
        }
        BigDecimal normalized = value.setScale(SCALE, ROUNDING);
        if (normalized.precision() > 19) {
            throw new IllegalArgumentException("Monetary value exceeds NUMERIC(19,2)");
        }
        return normalized;
    }

    public static BigDecimal increaseByPercentage(BigDecimal value, BigDecimal percentage) {
        BigDecimal factor = BigDecimal.ONE.add(
                percentage.movePointLeft(2)
        );
        return normalize(value.multiply(factor));
    }
}

