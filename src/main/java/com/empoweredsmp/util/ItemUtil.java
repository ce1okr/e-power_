package com.empoweredsmp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtil {

    /** Plain named item: a Level Fragment. Dropped by killing a player above level 0. */
    public static ItemStack buildLevelFragment(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Level Fragment", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(Keys.LEVEL_FRAGMENT, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** Plain named item: a Level Upgrader. Right-click to consume and gain one level (max 3). */
    public static ItemStack buildLevelUpgrader(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Level Upgrader", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(Keys.LEVEL_UPGRADER, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isLevelFragment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.LEVEL_FRAGMENT, PersistentDataType.BYTE);
    }

    public static boolean isLevelUpgrader(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.LEVEL_UPGRADER, PersistentDataType.BYTE);
    }

    /**
     * Ranger Level 2's "Custom Crossbow with Quick Charge X". Vanilla enchants cap
     * Quick Charge at level 5, so this is carried as flavor plus Unbreaking, and the
     * real "beyond vanilla" effect — instant reload, no charge delay at all — is
     * implemented in RangerCrossbowListener rather than through an enchant level.
     */
    public static ItemStack buildRangerCrossbow() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Quick Draw Crossbow", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Loads instantly — no charge time.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.QUICK_CHARGE, 5, true);
        meta.getPersistentDataContainer().set(Keys.RANGER_CROSSBOW, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRangerCrossbow(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.RANGER_CROSSBOW, PersistentDataType.BYTE);
    }
}
