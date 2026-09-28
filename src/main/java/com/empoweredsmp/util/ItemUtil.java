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

    /** Craftable item: rerolls the player to a uniformly random ability (1/8 each), resets to Level 0. */
    public static ItemStack buildRandomReroll(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Ability Reroll", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Right-click to reroll a random ability.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("(1/8 chance for each ability — resets you to Level 0)", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(Keys.REROLL_RANDOM, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRandomReroll(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.REROLL_RANDOM, PersistentDataType.BYTE);
    }

    /** Op-granted item: right-click opens a GUI to pick exactly which ability to reroll into. */
    public static ItemStack buildChoiceReroll(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Ability Selector", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Right-click to choose your new ability.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("(Resets you to Level 0)", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(Keys.REROLL_CHOICE, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isChoiceReroll(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.REROLL_CHOICE, PersistentDataType.BYTE);
    }

    /** Builds one clickable icon for the ability-picker GUI, tagged with which ability it represents. */
    public static ItemStack buildAbilityIcon(com.empoweredsmp.model.Ability ability, Material iconMaterial) {
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ability.name(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(Keys.ABILITY_ICON_TAG, PersistentDataType.STRING, ability.name());
        item.setItemMeta(meta);
        return item;
    }

    /** Reads which ability a picker-GUI icon represents, or null if it isn't one. */
    public static com.empoweredsmp.model.Ability readAbilityIcon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String stored = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.ABILITY_ICON_TAG, PersistentDataType.STRING);
        return stored == null ? null : com.empoweredsmp.model.Ability.fromString(stored);
    }
}
