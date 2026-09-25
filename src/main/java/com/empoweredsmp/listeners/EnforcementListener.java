package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.model.Ability;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Set;

/**
 * Blocks/removes items, enchants and potions that a player's ability+level
 * does not permit, and enforces the shared stack-size caps. Where an item
 * is already in an inventory (e.g. from another plugin or /give), it is
 * removed the next time it's interacted with or picked up rather than
 * scanned constantly, to keep this lightweight.
 */
public class EnforcementListener implements Listener {

    private static final Set<Material> NETHERITE_ARMOR = Set.of(
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
    private static final Set<Material> NETHERITE_TOOLS = Set.of(
            Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE);

    private final AbilityManager abilities;
    private final CooldownManager cooldowns;
    private final Config cfg;

    public EnforcementListener(AbilityManager abilities, CooldownManager cooldowns, Config cfg) {
        this.abilities = abilities;
        this.cooldowns = cooldowns;
        this.cfg = cfg;
    }

    private void deny(Player p, String reason) {
        p.sendMessage(Component.text(reason, NamedTextColor.RED));
    }

    // ---- Equipping netherite gear / swords / spears via click or interact ----

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
        if (!isAllowedItem(p, item)) {
            event.setCancelled(true);
            deny(p, "Your ability does not allow you to use that item.");
        }
    }

    private boolean isAllowedItem(Player p, ItemStack item) {
        Material type = item.getType();

        if (NETHERITE_ARMOR.contains(type) && !abilities.canUseNetheriteArmor(p)) return false;
        if (type == Material.NETHERITE_SWORD && !abilities.canUseNetheriteSword(p)) return false;
        // NOTE: Vanilla Minecraft has no "Spear" item. The Mace (added in 1.21) is the closest
        // vanilla analogue; if you want Mobility-only Mace gating, uncomment below.
        // if (type == Material.MACE && !abilities.canUseNetheriteSpear(p)) return false;

        if (NETHERITE_TOOLS.contains(type)) {
            if (!abilities.canUseNetheriteTool(p, type)) return false;
        }

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            for (var entry : meta.getEnchants().entrySet()) {
                Enchantment ench = entry.getKey();
                int lvl = entry.getValue();
                if (isProtection(ench) && lvl > effectiveProtectionCap(p)) return false;
                if (isSharpness(ench) && lvl > effectiveSharpnessCap(p)) return false;
                if (ench.equals(Enchantment.POWER) && lvl > effectivePowerCap(p)) return false;
            }
        }

        return true;
    }

    private boolean isProtection(Enchantment e) {
        return e.equals(Enchantment.PROTECTION);
    }

    private boolean isSharpness(Enchantment e) {
        return e.equals(Enchantment.SHARPNESS);
    }

    private int effectiveProtectionCap(Player p) {
        return abilities.canUseProtection4(p) ? cfg.protectionVitalityL1() : cfg.protectionNormal();
    }

    private int effectiveSharpnessCap(Player p) {
        return abilities.canUseSharpness5(p) ? cfg.sharpnessStrengthL2() : cfg.sharpnessNormal();
    }

    private int effectivePowerCap(Player p) {
        return abilities.canUsePower5(p) ? cfg.powerRangerL1() : cfg.powerNormal();
    }

    // ---- Potions ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION
                || item.getType() == Material.LINGERING_POTION) {
            if (isInvisibilityPotion(item) && !abilities.canUseInvisibilityPotion(p)) {
                event.setCancelled(true);
                deny(p, "Only Invisibility-ability players may drink Invisibility Potions.");
                return;
            }
            if (isFireResistancePotion(item) && !abilities.canUseFireResistancePotion(p)) {
                event.setCancelled(true);
                deny(p, "Only the Elemental ability may use Fire Resistance Potions.");
                return;
            }
        }
        if (item.getType() == Material.ENCHANTED_GOLDEN_APPLE && !abilities.canUseEnchantedGoldenApple(p)) {
            event.setCancelled(true);
            deny(p, "Only Vitality (level 2+) may eat Enchanted Golden Apples.");
        }
    }

    private boolean isInvisibilityPotion(ItemStack item) {
        return hasPotionEffectType(item, PotionEffectType.INVISIBILITY);
    }

    private boolean isFireResistancePotion(ItemStack item) {
        return hasPotionEffectType(item, PotionEffectType.FIRE_RESISTANCE);
    }

    private boolean hasPotionEffectType(ItemStack item, PotionEffectType type) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;
        if (meta.getBasePotionType() != null) {
            PotionType base = meta.getBasePotionType();
            if (base.getPotionEffects().stream().anyMatch(e -> e.getType().equals(type))) return true;
        }
        return meta.hasCustomEffects() && meta.getCustomEffects().stream().anyMatch(e -> e.getType().equals(type));
    }

    // ---- Potion duration multiplier (Prosperity 1.5x / 2x) ----

    @EventHandler(ignoreCancelled = true)
    public void onPotionApplied(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (event.getCause() != Cause.POTION_DRINK && event.getCause() != Cause.POTION_SPLASH
                && event.getCause() != Cause.AREA_EFFECT_CLOUD) return;
        if (event.getNewEffect() == null) return;
        if (!abilities.isProsperity(p)) return;

        double mult = abilities.isProsperityAtLeast(p, 2) ? cfg.potionMultiplierL2()
                : abilities.isProsperityAtLeast(p, 1) ? cfg.potionMultiplierL1() : 1.0;
        if (mult <= 1.0) return;

        var newEffect = event.getNewEffect();
        int scaledDuration = (int) Math.round(newEffect.getDuration() * mult);
        // Re-apply with the scaled duration next tick to avoid feedback looping this same event.
        org.bukkit.Bukkit.getScheduler().runTask(
                org.bukkit.Bukkit.getPluginManager().getPlugin("EmpoweredSMP"),
                () -> p.addPotionEffect(newEffect.withDuration(scaledDuration)));
    }

    // ---- Stack caps: golden apples, XP bottles, totems, cobwebs, wind charges ----

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
            case GOLDEN_APPLE -> abilities.isVitality(p) ? cfg.capGoldenApplesVitality() : cfg.capGoldenApplesNormal();
            case EXPERIENCE_BOTTLE -> abilities.isProsperity(p)
                    ? cfg.capXpBottlesProsperityStacks() * 64 : cfg.capXpBottlesNormal();
            case TOTEM_OF_UNDYING -> (abilities.isProsperityAtLeast(p, 3))
                    ? cfg.capTotemsProsperityL3() : cfg.capTotemsNormal();
            case COBWEB -> abilities.isMobility(p) ? cfg.capCobwebsMobility() : cfg.capCobwebsNormal();
            case WIND_CHARGE -> cfg.capWindCharges();
            case ENDER_PEARL -> cfg.capEnderPearlsMax();
            case ENCHANTED_GOLDEN_APPLE -> abilities.canUseEnchantedGoldenApple(p) ? cfg.capEnchantedGoldenApplesVitality() : 0;
            default -> -1;
        };
    }
}
