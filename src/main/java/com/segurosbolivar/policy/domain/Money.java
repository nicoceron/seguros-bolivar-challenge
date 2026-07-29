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
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal increaseByPercentage(BigDecimal value, BigDecimal percentage) {
        BigDecimal factor = BigDecimal.ONE.add(
                percentage.divide(BigDecimal.valueOf(100), 8, ROUNDING)
        );
        return normalize(value.multiply(factor));
    }
}

