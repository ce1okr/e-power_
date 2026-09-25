package com.empoweredsmp.model;

import java.util.UUID;

/**
 * Per-player persistent state. Ability is permanent once set (never null
 * again after /class assigns it). Level ranges 0-3. Death counter ranges
 * 0-4 quarters; hitting 4/4 drops the player one level (floored at 0, and
 * killing a level-0 player does not drop a fragment).
 */
public class PlayerData {

    private final UUID uuid;
    private Ability ability;
    private int level;
    private int deathCounter;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.ability = null;
        this.level = 0;
        this.deathCounter = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Ability getAbility() {
        return ability;
    }

    public void setAbility(Ability ability) {
        this.ability = ability;
    }

    public boolean hasAbility() {
        return ability != null;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(3, level));
    }

    public int getDeathCounter() {
        return deathCounter;
    }

    public void setDeathCounter(int deathCounter) {
        this.deathCounter = Math.max(0, Math.min(4, deathCounter));
    }

    /** Adds one death quarter. Returns true if this pushed the counter to 4/4 (a level was lost). */
    public boolean addDeathQuarter() {
        deathCounter++;
        if (deathCounter >= 4) {
            deathCounter = 0;
            if (level > 0) {
                level--;
            }
            return true;
        }
        return false;
    }

    /** Equipping/consuming a Level Fragment resets the death counter. */
    public void resetDeathCounter() {
        this.deathCounter = 0;
    }
}
