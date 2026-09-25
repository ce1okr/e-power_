package com.empoweredsmp.managers;

import com.empoweredsmp.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeRegistrar {

    private final Plugin plugin;
    private final Material fragmentMaterial;
    private final Material upgraderMaterial;

    public RecipeRegistrar(Plugin plugin, Material fragmentMaterial, Material upgraderMaterial) {
        this.plugin = plugin;
        this.fragmentMaterial = fragmentMaterial;
        this.upgraderMaterial = upgraderMaterial;
    }

    public void register() {
        NamespacedKey key = new NamespacedKey(plugin, "level_upgrader");
        ItemStack output = ItemUtil.buildLevelUpgrader(upgraderMaterial);

        ShapedRecipe recipe = new ShapedRecipe(key, output);
        recipe.shape("DFD", "FNF", "DFD");
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('N', Material.NETHER_STAR);
        // ExactChoice so only genuine (PDC-tagged) Level Fragments count, not any item
        // that happens to share the fragment's base material.
        ItemStack fragmentTemplate = ItemUtil.buildLevelFragment(fragmentMaterial);
        recipe.setIngredient('F', new RecipeChoice.ExactChoice(fragmentTemplate));

        plugin.getServer().addRecipe(recipe);
    }
}
