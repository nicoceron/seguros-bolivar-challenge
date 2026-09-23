// Purpose of this file: Keeps cent rounding and percentage calculations in one place.
package com.segurosbolivar.policy.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** Prevents creating an instance because this class only has static calculations. */
    private Money() {
    }

    /** Rounds money to two decimal places and rejects amounts too large to store. */
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

    /** Raises an amount by the given percentage and rounds the result. */
    public static BigDecimal increaseByPercentage(BigDecimal value, BigDecimal percentage) {
        BigDecimal factor = BigDecimal.ONE.add(
                percentage.movePointLeft(2)
        );
        return normalize(value.multiply(factor));
    }
}

