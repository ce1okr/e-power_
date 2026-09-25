package com.empoweredsmp.model;

/**
 * The eight permanent ability classes. A player has at most one, assigned
 * once by an admin via /class and never changed afterward.
 */
public enum Ability {
    PROSPERITY,
    INVISIBILITY,
    ELEMENTAL,
    VITALITY,
    RANGER,
    MOBILITY,
    DEFENSE,
    STRENGTH;

    public static Ability fromString(String s) {
        if (s == null) return null;
        try {
            return Ability.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
