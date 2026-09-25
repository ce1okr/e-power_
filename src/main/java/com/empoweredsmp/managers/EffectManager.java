package com.empoweredsmp.managers;

import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.BowEnchantChoice;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Every `effect-tick-interval-ticks`, re-applies permanent passive effects
 * for each online player based on their ability + level. Effects are
 * re-applied with a duration slightly longer than the tick interval so they
 * never visibly expire between ticks, and are removed immediately if the
 * player no longer qualifies (e.g. after losing a level).
 */
public class EffectManager extends BukkitRunnable {

    private final NamespacedKey maxHealthKey;
    private final NamespacedKey kbResistKey;

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final Config cfg;

    public EffectManager(Plugin plugin, AbilityManager abilities, Config cfg) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cfg = cfg;
        this.maxHealthKey = new NamespacedKey(plugin, "max_health_bonus");
        this.kbResistKey = new NamespacedKey(plugin, "kb_resist_bonus");
    }

    public void start() {
        runTaskTimer(plugin, 0L, cfg.effectTickInterval());
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            apply(p);
        }
    }

    private void give(Player p, PotionEffectType type, int amplifier, int durationTicks) {
        p.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false, false));
    }

    private void apply(Player p) {
        PlayerData d = abilities.get(p);
        Ability ability = d.getAbility();
        int level = d.getLevel();
        int dur = cfg.effectTickInterval() + 40; // outlives the gap between ticks

        // Max health (Vitality)
        double hearts = ability == Ability.VITALITY ? cfg.vitalityMaxHearts(level) : 10;
        setMaxHealth(p, hearts * 2.0);

        // Knockback resistance (Defense L1+)
        double kb = (ability == Ability.DEFENSE && level >= 1) ? cfg.defenseKnockbackResistL1() : 0.0;
        setKnockbackResist(p, kb);

        if (ability == null) return;

        switch (ability) {
            case INVISIBILITY -> {
                if (level >= 1) {
                    give(p, PotionEffectType.NIGHT_VISION, 0, dur);
                    give(p, PotionEffectType.INVISIBILITY, 0, dur);
                }
                if (level >= 2) give(p, PotionEffectType.SPEED, 0, dur);
            }
            case ELEMENTAL -> {
                if (level >= 1) {
                    give(p, PotionEffectType.FIRE_RESISTANCE, 0, dur);
                    give(p, PotionEffectType.WATER_BREATHING, 0, dur);
                    give(p, PotionEffectType.HASTE, level >= 2 ? 1 : 0, dur);
                }
                if (level >= 3) give(p, PotionEffectType.DOLPHINS_GRACE, 0, dur);
            }
            case MOBILITY -> {
                if (level >= 1) give(p, PotionEffectType.SPEED, level >= 2 ? 1 : 0, dur);
                if (level >= 2) give(p, PotionEffectType.WEAVING, 0, dur);
            }
            case DEFENSE -> {
                if (level >= 2) give(p, PotionEffectType.RESISTANCE, 0, dur);
            }
            case VITALITY -> {
                give(p, PotionEffectType.REGENERATION, 0, dur);
            }
            case STRENGTH -> {
                if (level >= 1) give(p, PotionEffectType.STRENGTH, level >= 2 ? 1 : 0, dur);
            }
            case RANGER -> {
                if (level >= 2) give(p, PotionEffectType.SPEED, 0, dur);
                if (level >= 1) syncBowEnchant(p, d.getBowEnchantChoice());
            }
            case PROSPERITY -> {
                if (level >= 2) give(p, PotionEffectType.HERO_OF_THE_VILLAGE, 4, dur); // "Hero of the Village V"
                if (level >= 3) give(p, PotionEffectType.HERO_OF_THE_VILLAGE, 9, dur); // "Hero of the Village X"
            }
            default -> { }
        }
    }

    private void setMaxHealth(Player p, double totalHp) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(maxHealthKey))
                .toList()
                .forEach(attr::removeModifier);
        double delta = totalHp - attr.getBaseValue();
        if (Math.abs(delta) > 0.001) {
            attr.addModifier(new AttributeModifier(maxHealthKey, delta, AttributeModifier.Operation.ADD_NUMBER));
        }
        if (p.getHealth() > attr.getValue()) {
            p.setHealth(attr.getValue());
        }
    }

    private void setKnockbackResist(Player p, double amount) {
        AttributeInstance attr = p.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(kbResistKey))
                .toList()
                .forEach(attr::removeModifier);
        if (amount > 0) {
            attr.addModifier(new AttributeModifier(kbResistKey, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /**
     * Ranger L1: keeps whatever bow the player is holding (main or off hand) carrying
     * exactly the enchant they've chosen via /bowchoice, removing the other one.
     * Doesn't touch crossbows — Ranger L2's custom crossbow is handled separately.
     */
    private void syncBowEnchant(Player p, BowEnchantChoice choice) {
        fixBowEnchant(p.getInventory().getItemInMainHand(), choice);
        fixBowEnchant(p.getInventory().getItemInOffHand(), choice);
    }

    private void fixBowEnchant(ItemStack item, BowEnchantChoice choice) {
        if (item == null || item.getType() != Material.BOW) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Enchantment want = choice == BowEnchantChoice.INFINITY ? Enchantment.INFINITY : Enchantment.MENDING;
        Enchantment other = choice == BowEnchantChoice.INFINITY ? Enchantment.MENDING : Enchantment.INFINITY;

        boolean changed = false;
        if (!meta.hasEnchant(want)) {
            meta.addEnchant(want, 1, true);
            changed = true;
        }
        if (meta.hasEnchant(other)) {
            meta.removeEnchant(other);
            changed = true;
        }
        if (changed) item.setItemMeta(meta);
    }
}
