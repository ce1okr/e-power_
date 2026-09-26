package com.empoweredsmp.managers;

import com.empoweredsmp.model.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import static org.bukkit.Material.*;

/**
 * One-time Level 0 kits, granted the moment /class assigns an ability.
 * NOTE: vanilla Minecraft has no "Spear" item type, so Mobility's "Lunge 3
 * Spear" is approximated here with a Trident tagged for a custom lunge
 * ability (see LungeListener). If your server has a custom spear item
 * plugin, swap SPEAR_MATERIAL below to match it.
 */
public class StarterKitManager {

    public static final org.bukkit.Material SPEAR_MATERIAL = TRIDENT;

    public void grantLevelZeroKit(Player p, Ability ability) {
        switch (ability) {
            case STRENGTH -> {
                ItemStack sword = new ItemStack(DIAMOND_SWORD);
                sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 4);
                p.getInventory().addItem(sword);
            }
            case DEFENSE -> {
                ItemStack chest = new ItemStack(DIAMOND_CHESTPLATE);
                chest.addUnsafeEnchantment(Enchantment.PROTECTION, 3);
                ItemStack legs = new ItemStack(DIAMOND_LEGGINGS);
                legs.addUnsafeEnchantment(Enchantment.PROTECTION, 3);
                p.getInventory().addItem(chest, legs);
            }
            case MOBILITY -> {
                ItemStack spear = new ItemStack(SPEAR_MATERIAL);
                ItemMeta meta = spear.getItemMeta();
                meta.displayName(Component.text("Lunge Spear", NamedTextColor.LIGHT_PURPLE));
                meta.lore(java.util.List.of(Component.text("Right-click to lunge forward. 15s cooldown.")
                        .color(NamedTextColor.GRAY)));
                spear.setItemMeta(meta);
                p.getInventory().addItem(spear);
            }
            case RANGER -> {
                ItemStack bow = new ItemStack(BOW);
                bow.addUnsafeEnchantment(Enchantment.POWER, 4);
                p.getInventory().addItem(bow, new ItemStack(ARROW, 64));
            }
            case VITALITY -> {
                // Max health handled continuously by EffectManager; Regeneration I is permanent (also there).
                p.sendMessage(Component.text("Vitality grants +1 heart and permanent Regeneration I.",
                        NamedTextColor.GREEN));
            }
            case ELEMENTAL -> {
                p.sendMessage(Component.text("Elemental grants early Nether access.", NamedTextColor.GREEN));
            }
            case INVISIBILITY -> {
                for (int i = 0; i < 2; i++) {
                    p.getInventory().addItem(buildInvisPotion());
                }
            }
            case PROSPERITY -> {
                p.getInventory().addItem(new ItemStack(VILLAGER_SPAWN_EGG, 2));
            }
        }
    }

    /**
     * Granted the moment a player's level increases to newLevel (via the Level
     * Upgrader), on top of whatever they already have. Ranger L2 grants a custom
     * crossbow; Defense L1 grants an Unbreakable Shield (kept unbreakable
     * afterward by EffectManager even if replaced). Other abilities' level-ups
     * are all passive stat changes handled continuously by EffectManager.
     */
    public void grantLevelUpKit(Player p, Ability ability, int newLevel) {
        if (ability == Ability.RANGER && newLevel == 2) {
            p.getInventory().addItem(com.empoweredsmp.util.ItemUtil.buildRangerCrossbow());
            p.sendMessage(Component.text("You've been given a Quick Draw Crossbow — "
                    + "right-click to load it instantly, no charge time.", NamedTextColor.YELLOW));
        }
        if (ability == Ability.DEFENSE && newLevel == 1) {
            ItemStack shield = new ItemStack(SHIELD);
            ItemMeta meta = shield.getItemMeta();
            meta.setUnbreakable(true);
            shield.setItemMeta(meta);
            p.getInventory().addItem(shield);
            p.sendMessage(Component.text("You've been given an Unbreakable Shield.", NamedTextColor.YELLOW));
        }
    }

    private ItemStack buildInvisPotion() {
        ItemStack potion = new ItemStack(POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(PotionType.INVISIBILITY);
        // Vanilla base invisibility potions last 3 minutes; extend to 8 minutes via a custom effect.
        meta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY, 8 * 60 * 20, 0, false, true, true), true);
        meta.displayName(Component.text("Invisibility Potion (8:00)", NamedTextColor.DARK_PURPLE));
        potion.setItemMeta(meta);
        return potion;
    }
}
