package com.empoweredsmp.model;

import org.bukkit.Material;

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

    /** Ranger L1: which enchant to keep applied to whatever bow they hold. Defaults to MENDING. */
    private BowEnchantChoice bowEnchantChoice = BowEnchantChoice.MENDING;

    /** Prosperity L2: the two netherite tool types they picked, locked in once set via /toolchoice. */
    private Material prosperityTool1;
    private Material prosperityTool2;

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

    public BowEnchantChoice getBowEnchantChoice() {
        return bowEnchantChoice;
    }

    public void setBowEnchantChoice(BowEnchantChoice choice) {
        this.bowEnchantChoice = choice == null ? BowEnchantChoice.MENDING : choice;
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
}
