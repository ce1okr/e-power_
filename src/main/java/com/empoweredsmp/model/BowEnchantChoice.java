package com.empoweredsmp.model;

/**
 * Ranger Level 1: "Infinity OR Mending on any bow you hold (choose each time
 * you hold a bow)". Implemented as a standing preference the player sets
 * with /bowchoice, applied to whatever bow is currently in their hand.
 */
public enum BowEnchantChoice {
    INFINITY,
    MENDING;

    public static BowEnchantChoice fromString(String s) {
        if (s == null) return null;
        try {
            return BowEnchantChoice.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
