package com.empoweredsmp.managers;

import com.empoweredsmp.model.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import static org.bukkit.Material.*;

/**
 * Level 0 kits, granted the moment an ability is assigned (/class).
 * Mobility's spear is the real vanilla Spear with the real Lunge enchantment;
 * the 15-second cooldown is enforced by LungeListener.
 */
public class StarterKitManager {

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
                ItemStack spear = new ItemStack(DIAMOND_SPEAR);
                spear.addUnsafeEnchantment(Enchantment.LUNGE, 3);
                p.getInventory().addItem(spear);
            }
            case RANGER -> {
                ItemStack bow = new ItemStack(BOW);
                bow.addUnsafeEnchantment(Enchantment.POWER, 4);
                p.getInventory().addItem(bow, new ItemStack(ARROW, 64));
            }
            case VITALITY -> {
                // 11 hearts + Regeneration I are applied continuously by EffectManager.
                p.sendMessage(Component.text("Vitality grants 11 hearts and Regeneration I.",
                        NamedTextColor.GREEN));
            }
            case ELEMENTAL -> {
                p.sendMessage(Component.text("Elemental can enter the Nether before everyone else.",
                        NamedTextColor.GREEN));
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
