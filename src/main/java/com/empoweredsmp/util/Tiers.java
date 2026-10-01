package com.empoweredsmp.util;

/**
 * Players progress Level 0 (starting state, kit only) -> Low Tier -> High Tier.
 * Internally that's just the numbers 0, 1 and 2.
 */
public final class Tiers {

    public static final int LEVEL_0 = 0;
    public static final int LOW = 1;
    public static final int HIGH = 2;
    public static final int MAX = HIGH;

    private Tiers() {}

    public static String name(int level) {
        return switch (level) {
            case 0 -> "Level 0";
            case 1 -> "Low Tier";
            default -> "High Tier";
        };
    }
}
