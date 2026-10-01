package com.empoweredsmp.model;

import com.empoweredsmp.util.Tiers;
import org.bukkit.Material;

import java.util.UUID;

/**
 * Per-player persistent state. Level is 0 (starting state), 1 (Low Tier) or
 * 2 (High Tier). The death counter runs 0-4; hitting 4/4 drops the player one
 * tier (floored at Level 0).
 */
public class PlayerData {

    private final UUID uuid;
    private Ability ability;
    private int level;
    private int deathCounter;

    /** Prosperity Low Tier: the two netherite tool types they picked, locked in via /toolchoice. */
    private Material prosperityTool1;
    private Material prosperityTool2;

    /** Epoch millis until which this player (as a Prosperity curser) can't curse again. */
    private long curseCooldownUntil;

    /** Epoch millis until which this player is cursed (0 = not cursed). */
    private long cursedUntil;

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

    /** Clamped to 0..2. Old saved data with level 3 therefore loads as High Tier. */
    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(Tiers.MAX, level));
    }

    public int getDeathCounter() {
        return deathCounter;
    }

    public void setDeathCounter(int deathCounter) {
        this.deathCounter = Math.max(0, Math.min(4, deathCounter));
    }

    /** Adds one death quarter. Returns true if this pushed the counter to 4/4 (a tier was lost). */
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

    /** Consuming a Level Fragment resets the death counter. */
    public void resetDeathCounter() {
        this.deathCounter = 0;
    }

    public Material getProsperityTool1() {
        return prosperityTool1;
    }

    public Material getProsperityTool2() {
        return prosperityTool2;
    }

    public boolean hasChosenProsperityTools() {
        return prosperityTool1 != null && prosperityTool2 != null;
    }

    /** Locks in the two netherite tool types. Does nothing if already chosen. */
    public boolean setProsperityTools(Material tool1, Material tool2) {
        if (hasChosenProsperityTools()) return false;
        this.prosperityTool1 = tool1;
        this.prosperityTool2 = tool2;
        return true;
    }

    /** Used when an op changes someone's ability: back to Level 0 and clear ability-specific choices. */
    public void resetForAbilityChange() {
        this.level = 0;
        this.deathCounter = 0;
        this.prosperityTool1 = null;
        this.prosperityTool2 = null;
    }

    public long getCurseCooldownUntil() {
        return curseCooldownUntil;
    }

    public void setCurseCooldownUntil(long curseCooldownUntil) {
        this.curseCooldownUntil = curseCooldownUntil;
    }

    public long getCursedUntil() {
        return cursedUntil;
    }

    public void setCursedUntil(long cursedUntil) {
        this.cursedUntil = cursedUntil;
    }

    public boolean isCursed(long nowMillis) {
        return cursedUntil > nowMillis;
    }
}
