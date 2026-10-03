package com.empoweredsmp.managers;

import com.empoweredsmp.util.Config;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

/**
 * Decides whether an item is "illegal" for a player: netherite gear their ability
 * can't use, or enchant levels above their cap. Illegal items can still be held,
 * moved, stored and dropped; they just can't be used or worn (see EnforcementListener
 * and IllegalGearSweeper).
 */
public class ItemRules {

    private static final Set<Material> NETHERITE_ARMOR = Set.of(
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
    private static final Set<Material> NETHERITE_TOOLS = Set.of(
            Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE);

    private final AbilityManager abilities;
    private final Config cfg;

    public ItemRules(AbilityManager abilities, Config cfg) {
        this.abilities = abilities;
        this.cfg = cfg;
    }

    /** True if this player may use/wear the item. */
    public boolean isAllowed(Player p, ItemStack item) {
        if (item == null) return true;
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
                if (isProtectionFamily(ench) && lvl > protectionCap()) return false;
                if (isSharpnessFamily(ench) && lvl > sharpnessCap(p)) return false;
                if (ench.equals(Enchantment.POWER) && lvl > powerCap(p)) return false;
            }
        }
        return true;
    }

    /** The armor slot this vanilla armor piece is worn in, or null if it isn't an armor piece. */
    public static EquipmentSlot armorSlotOf(ItemStack item) {
        if (item == null) return null;
        String name = item.getType().name();
        if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return null;
    }

    private boolean isProtectionFamily(Enchantment e) {
        return e.equals(Enchantment.PROTECTION) || e.equals(Enchantment.BLAST_PROTECTION)
                || e.equals(Enchantment.PROJECTILE_PROTECTION) || e.equals(Enchantment.FIRE_PROTECTION);
    }

    private boolean isSharpnessFamily(Enchantment e) {
        return e.equals(Enchantment.SHARPNESS) || e.equals(Enchantment.SMITE)
                || e.equals(Enchantment.BANE_OF_ARTHROPODS);
    }

    /** Protection-family cap is the same for every ability and tier (no Vitality exception). */
    private int protectionCap() {
        return cfg.protectionNormal();
    }

    private int sharpnessCap(Player p) {
        return abilities.canUseSharpness5(p) ? cfg.sharpnessStrength() : cfg.sharpnessNormal();
    }

    private int powerCap(Player p) {
        return abilities.canUsePower5(p) ? cfg.powerRanger() : cfg.powerNormal();
    }
}
