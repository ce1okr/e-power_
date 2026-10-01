package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Set;

/**
 * Enforces "only this ability can use X" rules, enchant caps and item caps.
 * Disallowed items/effects are blocked when used, equipped or picked up.
 */
public class EnforcementListener implements Listener {

    private static final Set<Material> NETHERITE_ARMOR = Set.of(
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
    private static final Set<Material> NETHERITE_TOOLS = Set.of(
            Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE);

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final Config cfg;

    public EnforcementListener(Plugin plugin, AbilityManager abilities, Config cfg) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cfg = cfg;
    }

    private void deny(Player p, String reason) {
        p.sendMessage(Component.text(reason, NamedTextColor.RED));
    }

    // ---- Equipping / using gear ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (!isAllowedItem(p, item)) {
            event.setCancelled(true);
            deny(p, "Your ability does not allow you to use that item.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (isPvpFirework(item) && !abilities.canUseRangerGear(p)) {
            event.setCancelled(true);
            deny(p, "Only Rangers can use PvP firework rockets.");
            return;
        }
        if (!isAllowedItem(p, item)) {
            event.setCancelled(true);
            deny(p, "Your ability does not allow you to use that item.");
        }
    }

    private boolean isAllowedItem(Player p, ItemStack item) {
        Material type = item.getType();

        if (NETHERITE_ARMOR.contains(type) && !abilities.canUseNetheriteArmor(p)) return false;
        if (type == Material.NETHERITE_SWORD && !abilities.canUseNetheriteSword(p)) return false;
        if (type == Material.NETHERITE_SPEAR && !abilities.canUseNetheriteSpear(p)) return false;
        if (NETHERITE_TOOLS.contains(type) && !abilities.canUseNetheriteTool(p, type)) return false;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            for (var entry : meta.getEnchants().entrySet()) {
                Enchantment ench = entry.getKey();
                int lvl = entry.getValue();
                if (isProtectionFamily(ench) && lvl > effectiveProtectionCap(p)) return false;
                if (isSharpnessFamily(ench) && lvl > effectiveSharpnessCap(p)) return false;
                if (ench.equals(Enchantment.POWER) && lvl > effectivePowerCap(p)) return false;
            }
        }
        return true;
    }

    private boolean isProtectionFamily(Enchantment e) {
        return e.equals(Enchantment.PROTECTION) || e.equals(Enchantment.BLAST_PROTECTION)
                || e.equals(Enchantment.PROJECTILE_PROTECTION) || e.equals(Enchantment.FIRE_PROTECTION);
    }

    private boolean isSharpnessFamily(Enchantment e) {
        return e.equals(Enchantment.SHARPNESS) || e.equals(Enchantment.SMITE)
                || e.equals(Enchantment.BANE_OF_ARTHROPODS);
    }

    private int effectiveProtectionCap(Player p) {
        return abilities.canUseProtection4(p) ? cfg.protectionVitality() : cfg.protectionNormal();
    }

    private int effectiveSharpnessCap(Player p) {
        return abilities.canUseSharpness5(p) ? cfg.sharpnessStrength() : cfg.sharpnessNormal();
    }

    private int effectivePowerCap(Player p) {
        return abilities.canUsePower5(p) ? cfg.powerRanger() : cfg.powerNormal();
    }

    // ---- Ranger-only ammo: tipped arrows and PvP (explosive) firework rockets ----

    /** A firework rocket with explosion stars (the kind used for crossbow PvP), not a plain flight rocket. */
    private boolean isPvpFirework(ItemStack item) {
        return item != null && item.getType() == Material.FIREWORK_ROCKET
                && item.getItemMeta() instanceof FireworkMeta meta && meta.hasEffects();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (abilities.canUseRangerGear(p)) return;
        ItemStack ammo = event.getConsumable();
        if (ammo == null) return;
        if (ammo.getType() == Material.TIPPED_ARROW) {
            event.setCancelled(true);
            deny(p, "Only Rangers can shoot tipped arrows.");
        } else if (isPvpFirework(ammo)) {
            event.setCancelled(true);
            deny(p, "Only Rangers can shoot PvP firework rockets.");
        }
    }

    // ---- Potions and food ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION
                || item.getType() == Material.LINGERING_POTION) {
            if (hasPotionEffectType(item, PotionEffectType.INVISIBILITY) && !abilities.canUseInvisibilityPotion(p)) {
                event.setCancelled(true);
                deny(p, "Only the Invisibility ability may use Invisibility Potions.");
                return;
            }
            if (hasPotionEffectType(item, PotionEffectType.FIRE_RESISTANCE) && !abilities.canUseFireResistancePotion(p)) {
                event.setCancelled(true);
                deny(p, "Only the Elemental ability may use Fire Resistance Potions.");
                return;
            }
            if (isTurtleMasterPotion(item) && !abilities.canUseTurtleMaster(p)) {
                event.setCancelled(true);
                deny(p, "Only the Defense ability may use Turtle Master Potions.");
                return;
            }
        }
        if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE && !abilities.canUseEnchantedGoldenApple(p)) {
            event.setCancelled(true);
            deny(p, "Only the Vitality ability may eat Enchanted Golden Apples.");
        }
    }

    private boolean isTurtleMasterPotion(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        PotionType base = meta.getBasePotionType();
        if (base == null) return false;
        return base == PotionType.TURTLE_MASTER || base == PotionType.LONG_TURTLE_MASTER
                || base == PotionType.STRONG_TURTLE_MASTER;
    }

    private boolean hasPotionEffectType(ItemStack item, PotionEffectType type) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        PotionType base = meta.getBasePotionType();
        if (base != null && base.getPotionEffects().stream().anyMatch(e -> e.getType().equals(type))) return true;
        return meta.hasCustomEffects() && meta.getCustomEffects().stream().anyMatch(e -> e.getType().equals(type));
    }

    // ---- Effect exclusivity, amplifier caps, Prosperity potion multiplier ----

    @EventHandler(ignoreCancelled = true)
    public void onPotionApplied(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return;
        PotionEffectType type = newEffect.getType();

        // These apply to every source (potion, beacon, another plugin, /effect).
        if (type.equals(PotionEffectType.FIRE_RESISTANCE) && !abilities.canUseFireResistancePotion(p)) {
            event.setCancelled(true);
            return;
        }
        if (type.equals(PotionEffectType.INVISIBILITY) && !abilities.canUseInvisibilityPotion(p)) {
            event.setCancelled(true);
            return;
        }
        if (type.equals(PotionEffectType.WEAVING) && !abilities.canUseWeaving(p)) {
            event.setCancelled(true);
            return;
        }
        if (type.equals(PotionEffectType.STRENGTH) && newEffect.getAmplifier() >= 1
                && !abilities.canUseStrength2(p)) {
            event.setCancelled(true);
            capAmplifierToZero(p, newEffect);
            return;
        }
        if (type.equals(PotionEffectType.SPEED) && newEffect.getAmplifier() >= 1
                && !abilities.canUseSpeed2(p)) {
            event.setCancelled(true);
            capAmplifierToZero(p, newEffect);
            return;
        }

        // Prosperity: potion effects last longer (only for real potions).
        if (event.getCause() != Cause.POTION_DRINK && event.getCause() != Cause.POTION_SPLASH
                && event.getCause() != Cause.AREA_EFFECT_CLOUD) return;
        if (!abilities.isProsperityAtLeast(p, 1)) return;

        double mult = cfg.prosperityPotionMultiplier(abilities.levelOf(p));
        if (mult <= 1.0) return;

        int scaledDuration = (int) Math.round(newEffect.getDuration() * mult);
        // Re-apply with the longer duration next tick (cause is PLUGIN then, so it won't scale twice).
        Bukkit.getScheduler().runTask(plugin, () -> p.addPotionEffect(newEffect.withDuration(scaledDuration)));
    }

    /** Downgrades e.g. Strength II to Strength I instead of denying the effect entirely. */
    private void capAmplifierToZero(Player p, PotionEffect original) {
        PotionEffect capped = new PotionEffect(original.getType(), original.getDuration(), 0,
                original.isAmbient(), original.hasParticles(), original.hasIcon());
        Bukkit.getScheduler().runTask(plugin, () -> p.addPotionEffect(capped));
    }

    // ---- Item caps ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        ItemStack picked = event.getItem().getItemStack();
        int cap = capFor(p, picked.getType());
        if (cap < 0) return; // uncapped item type
        int currentlyHeld = countInInventory(p, picked.getType());
        if (currentlyHeld + picked.getAmount() > cap) {
            event.setCancelled(true);
            deny(p, "You're already carrying the maximum amount of that item.");
        }
    }

    private int countInInventory(Player p, Material type) {
        int total = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == type) total += item.getAmount();
        }
        return total;
    }

    private int capFor(Player p, Material type) {
        return switch (type) {
            case GOLDEN_APPLE -> abilities.isVitalityAtLeast(p, 1)
                    ? cfg.capGoldenApplesVitality() : cfg.capGoldenApplesNormal();
            case ENCHANTED_GOLDEN_APPLE -> abilities.isVitalityAtLeast(p, 1)
                    ? cfg.capEnchantedGoldenApples(abilities.levelOf(p)) : 0;
            case EXPERIENCE_BOTTLE -> abilities.isProsperityAtLeast(p, 1)
                    ? cfg.capXpBottlesProsperityStacks() * 64 : cfg.capXpBottlesNormal();
            case TOTEM_OF_UNDYING -> abilities.isProsperityAtLeast(p, 1)
                    ? cfg.capTotemsProsperity() : cfg.capTotemsNormal();
            case COBWEB -> abilities.isMobilityAtLeast(p, 1)
                    ? cfg.capCobwebsMobility() : cfg.capCobwebsNormal();
            case WIND_CHARGE -> cfg.capWindCharges();
            case ENDER_PEARL -> cfg.capEnderPearlsMax();
            default -> -1;
        };
    }
}
