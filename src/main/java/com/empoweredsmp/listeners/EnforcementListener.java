package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.ItemRules;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces "only this ability can use X" rules, enchant caps and item caps.
 *
 * An illegal item (netherite gear or enchant levels you aren't allowed) can still be
 * held, moved around, stored and dropped. What's blocked is USING it: wearing illegal
 * armor, attacking / breaking blocks / shooting / right-click-using with it. Which items
 * count as illegal is decided by ItemRules.
 */
public class EnforcementListener implements Listener {

    private final Plugin plugin;
    private final AbilityManager abilities;
    private final Config cfg;
    private final ItemRules rules;

    /** Throttle for the "can't use that item" action-bar message. */
    private final Map<UUID, Long> lastItemWarning = new HashMap<>();

    public EnforcementListener(Plugin plugin, AbilityManager abilities, Config cfg, ItemRules rules) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.cfg = cfg;
        this.rules = rules;
    }

    private void deny(Player p, String reason) {
        p.sendMessage(Component.text(reason, NamedTextColor.RED));
    }

    private void denyItem(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastItemWarning.get(p.getUniqueId());
        if (last != null && now - last < 1500) return;
        lastItemWarning.put(p.getUniqueId(), now);
        p.sendActionBar(Component.text("You can't use that item. It's illegal for your ability.", NamedTextColor.RED));
    }

    // ---- Wearing illegal armor (everything else about the item stays allowed) ----

    private boolean isSlotEmpty(Player p, EquipmentSlot slot) {
        PlayerInventory inv = p.getInventory();
        ItemStack current = switch (slot) {
            case HEAD -> inv.getHelmet();
            case CHEST -> inv.getChestplate();
            case LEGS -> inv.getLeggings();
            case FEET -> inv.getBoots();
            default -> null;
        };
        return current == null || current.getType() == Material.AIR;
    }

    /** Only blocks a click when it would put illegal armor into an armor slot. Moving/dropping is untouched. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        boolean armorSlotClicked = event.getSlotType() == InventoryType.SlotType.ARMOR;
        ItemStack incoming = null; // the item this click would put into an armor slot

        switch (event.getClick()) {
            case NUMBER_KEY -> {
                // Hotbar-key swap onto an armor slot.
                if (armorSlotClicked && event.getHotbarButton() >= 0) {
                    incoming = p.getInventory().getItem(event.getHotbarButton());
                }
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                // Shift-click in your own inventory auto-equips armor if that slot is empty.
                if (!armorSlotClicked && event.getClickedInventory() instanceof PlayerInventory) {
                    ItemStack clicked = event.getCurrentItem();
                    EquipmentSlot slot = ItemRules.armorSlotOf(clicked);
                    if (slot != null && isSlotEmpty(p, slot)) {
                        incoming = clicked;
                    }
                }
            }
            default -> {
                // Normal click: whatever is on the cursor goes into the armor slot.
                if (armorSlotClicked) {
                    incoming = event.getCursor();
                }
            }
        }

        if (incoming == null || incoming.getType() == Material.AIR) return;
        if (ItemRules.armorSlotOf(incoming) == null) return; // only armor pieces can be worn
        if (!rules.isAllowed(p, incoming)) {
            event.setCancelled(true);
            denyItem(p);
        }
    }

    /** Dragging illegal armor across an armor slot. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        ItemStack dragged = event.getOldCursor();
        if (ItemRules.armorSlotOf(dragged) == null || rules.isAllowed(p, dragged)) return;
        for (int rawSlot : event.getRawSlots()) {
            if (event.getView().getSlotType(rawSlot) == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                denyItem(p);
                return;
            }
        }
    }

    /** Dispensers equipping armor onto a player. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (!(event.getTargetEntity() instanceof Player p)) return;
        if (!rules.isAllowed(p, event.getItem())) {
            event.setCancelled(true);
        }
    }

    // ---- Using illegal items ----

    /**
     * Right-click / use. Only the item's own use is denied (this also stops right-click
     * equipping armor and drawing a bow); opening chests, doors etc. still works.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (isPvpFirework(item) && !abilities.canUseRangerGear(p)) {
            event.setUseItemInHand(Event.Result.DENY);
            deny(p, "Only Rangers can use PvP firework rockets.");
            return;
        }
        if (!rules.isAllowed(p, item)) {
            event.setUseItemInHand(Event.Result.DENY);
            denyItem(p);
        }
    }

    /** Attacking with an illegal item in hand. Runs first so other listeners skip the cancelled hit. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) return;
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) return;
        if (!rules.isAllowed(p, held)) {
            event.setCancelled(true);
            denyItem(p);
        }
    }

    /** Breaking blocks with an illegal tool in hand. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) return;
        if (!rules.isAllowed(p, held)) {
            event.setCancelled(true);
            denyItem(p);
        }
    }

    // ---- Shooting: illegal bows, and Ranger-only ammo (tipped arrows, PvP firework rockets) ----

    /** A firework rocket with explosion stars (the kind used for crossbow PvP), not a plain flight rocket. */
    private boolean isPvpFirework(ItemStack item) {
        return item != null && item.getType() == Material.FIREWORK_ROCKET
                && item.getItemMeta() instanceof FireworkMeta meta && meta.hasEffects();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        ItemStack bow = event.getBow();
        if (bow != null && !rules.isAllowed(p, bow)) {
            event.setCancelled(true);
            denyItem(p);
            return;
        }

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
