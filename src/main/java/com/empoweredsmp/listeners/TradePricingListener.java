package com.empoweredsmp.listeners;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.util.Config;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prosperity trade pricing, applied while a Prosperity player has the trade
 * screen open: Low Tier pays 50% of the normal emerald price, High Tier pays
 * exactly 1 emerald. Only trades that cost emeralds are touched. The villager's
 * normal special prices are put back when the screen closes, so other players
 * (and wandering traders) aren't affected.
 */
public class TradePricingListener implements Listener {

    private final AbilityManager abilities;
    private final Config cfg;
    /** player -> the special price of every offer before we changed it */
    private final Map<UUID, int[]> originalSpecialPrices = new HashMap<>();

    public TradePricingListener(AbilityManager abilities, Config cfg) {
        this.abilities = abilities;
        this.cfg = cfg;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        if (!abilities.isProsperityAtLeast(p, 1)) return;

        Merchant merchant = inventory.getMerchant();
        if (merchant == null) return;
        int level = abilities.levelOf(p);

        List<MerchantRecipe> recipes = merchant.getRecipes();
        int[] originals = new int[recipes.size()];
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            originals[i] = recipe.getSpecialPrice();

            List<ItemStack> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty() || ingredients.get(0).getType() != Material.EMERALD) continue;

            // Price with no special discount, then pick the special price that lands on our target.
            recipe.setSpecialPrice(0);
            ItemStack adjusted = recipe.getAdjustedIngredient1();
            if (adjusted == null) {
                recipe.setSpecialPrice(originals[i]);
                continue;
            }
            int current = adjusted.getAmount();
            int target = level >= 2
                    ? 1
                    : Math.max(1, (int) Math.round(current * (1.0 - cfg.prosperityTradeDiscountLow())));
            recipe.setSpecialPrice(target - current);
            merchant.setRecipe(i, recipe);
        }
        originalSpecialPrices.put(p.getUniqueId(), originals);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) return;
        int[] originals = originalSpecialPrices.remove(event.getPlayer().getUniqueId());
        if (originals == null) return;

        Merchant merchant = inventory.getMerchant();
        if (merchant == null) return;
        List<MerchantRecipe> recipes = merchant.getRecipes();
        int count = Math.min(originals.length, recipes.size());
        for (int i = 0; i < count; i++) {
            MerchantRecipe recipe = recipes.get(i);
            if (recipe.getSpecialPrice() != originals[i]) {
                recipe.setSpecialPrice(originals[i]);
                merchant.setRecipe(i, recipe);
            }
        }
    }
}
