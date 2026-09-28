package com.empoweredsmp.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class Keys {

    public static NamespacedKey LEVEL_FRAGMENT;
    public static NamespacedKey LEVEL_UPGRADER;
    public static NamespacedKey RANGER_CROSSBOW;
    public static NamespacedKey REROLL_RANDOM;
    public static NamespacedKey REROLL_CHOICE;
    public static NamespacedKey ABILITY_ICON_TAG;

    public static void init(Plugin plugin) {
        LEVEL_FRAGMENT = new NamespacedKey(plugin, "level_fragment");
        LEVEL_UPGRADER = new NamespacedKey(plugin, "level_upgrader");
        RANGER_CROSSBOW = new NamespacedKey(plugin, "ranger_crossbow");
        REROLL_RANDOM = new NamespacedKey(plugin, "reroll_random");
        REROLL_CHOICE = new NamespacedKey(plugin, "reroll_choice");
        ABILITY_ICON_TAG = new NamespacedKey(plugin, "ability_icon");
    }
}
