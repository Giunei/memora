package com.memora.languages.domain;

/**
 * Common European Framework of Reference proficiency levels, in ascending order.
 *
 * <p>Declaration order is the comparison order, which {@link #isAtLeast} relies
 * on. New levels must never be inserted in the middle.
 */
public enum CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2;

    public boolean isAtLeast(CefrLevel other) {
        return ordinal() >= other.ordinal();
    }
}
