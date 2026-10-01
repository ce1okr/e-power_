package com.empoweredsmp.managers;

import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Central place for "is this player allowed to use X" questions, derived from
 * their ability + tier (0 = Level 0, 1 = Low Tier, 2 = High Tier).
 */
public class AbilityManager {

    private final DataManager data;

    public AbilityManager(DataManager data) {
        this.data = data;
    }

    public PlayerData get(Player p) {
        return data.get(p.getUniqueId());
    }

    public PlayerData get(UUID uuid) {
        return data.get(uuid);
    }

    /** Assign an ability to a player who has none. Fails (returns false) if they already have one. */
    public boolean assign(Player p, Ability ability) {
        PlayerData d = get(p);
        if (d.hasAbility()) return false;
        d.setAbility(ability);
        d.setLevel(0);
        data.save(p.getUniqueId());
        return true;
    }

    public Ability abilityOf(Player p) {
        return get(p).getAbility();
    }

    public int levelOf(Player p) {
        return get(p).getLevel();
    }

    /** Persists whatever is currently in this player's PlayerData. */
    public void save(Player p) {
        data.save(p.getUniqueId());
    }

    private boolean is(Player p, Ability ability, int minLevel) {
        PlayerData d = get(p);
        return d.getAbility() == ability && d.getLevel() >= minLevel;
    }

    // ---- Netherite ----
    public boolean canUseNetheriteArmor(Player p) { return is(p, Ability.DEFENSE, 2); }
    public boolean canUseNetheriteSword(Player p) { return is(p, Ability.STRENGTH, 1); }
    public boolean canUseNetheriteSpear(Player p) { return is(p, Ability.MOBILITY, 1); }

    /**
     * Prosperity High Tier may use every netherite tool. Prosperity Low Tier may
     * only use the two tool types they locked in with /toolchoice. Everyone else, no.
     */
    public boolean canUseNetheriteTool(Player p, Material toolType) {
        PlayerData d = get(p);
        if (d.getAbility() != Ability.PROSPERITY) return false;
        if (d.getLevel() >= 2) return true;
        if (d.getLevel() == 1) {
            return toolType.equals(d.getProsperityTool1()) || toolType.equals(d.getProsperityTool2());
        }
        return false;
    }

    /** Eligible to run /toolchoice: Prosperity Low Tier who hasn't chosen yet. */
    public boolean canChooseProsperityTools(Player p) {
        PlayerData d = get(p);
        return d.getAbility() == Ability.PROSPERITY && d.getLevel() == 1 && !d.hasChosenProsperityTools();
    }

    // ---- Potions / effects exclusivity ----
    public boolean canUseFireResistancePotion(Player p) { return is(p, Ability.ELEMENTAL, 1); }
    public boolean canUseInvisibilityPotion(Player p) { return is(p, Ability.INVISIBILITY, 0); }
    public boolean canUseTurtleMaster(Player p) { return is(p, Ability.DEFENSE, 1); }
    public boolean canUseEnchantedGoldenApple(Player p) { return is(p, Ability.VITALITY, 1); }
    public boolean canUseWeaving(Player p) { return is(p, Ability.MOBILITY, 1); }
    public boolean canUseSpeed2(Player p) { return is(p, Ability.MOBILITY, 2); }
    public boolean canUseStrength2(Player p) { return is(p, Ability.STRENGTH, 2); }

    // ---- Enchant tiers ----
    public boolean canUseProtection4(Player p) { return is(p, Ability.VITALITY, 1); }
    public boolean canUseSharpness5(Player p) { return is(p, Ability.STRENGTH, 2); }
    public boolean canUsePower5(Player p) { return is(p, Ability.RANGER, 1); }

    // ---- Ranger-only gear: tipped arrows and PvP (explosive) firework rockets ----
    public boolean canUseRangerGear(Player p) { return is(p, Ability.RANGER, 1); }

    // ---- Ability buckets used by item caps ----
    public boolean isProsperity(Player p) { return abilityOf(p) == Ability.PROSPERITY; }

    public boolean isElementalAtLeast(Player p, int lvl) { return is(p, Ability.ELEMENTAL, lvl); }
    public boolean isInvisibilityAtLeast(Player p, int lvl) { return is(p, Ability.INVISIBILITY, lvl); }
    public boolean isVitalityAtLeast(Player p, int lvl) { return is(p, Ability.VITALITY, lvl); }
    public boolean isDefenseAtLeast(Player p, int lvl) { return is(p, Ability.DEFENSE, lvl); }
    public boolean isStrengthAtLeast(Player p, int lvl) { return is(p, Ability.STRENGTH, lvl); }
    public boolean isMobilityAtLeast(Player p, int lvl) { return is(p, Ability.MOBILITY, lvl); }
    public boolean isRangerAtLeast(Player p, int lvl) { return is(p, Ability.RANGER, lvl); }
    public boolean isProsperityAtLeast(Player p, int lvl) { return is(p, Ability.PROSPERITY, lvl); }
}
