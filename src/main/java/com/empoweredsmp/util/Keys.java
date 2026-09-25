package com.empoweredsmp.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class Keys {

    public static NamespacedKey LEVEL_FRAGMENT;
    public static NamespacedKey LEVEL_UPGRADER;

    public static void init(Plugin plugin) {
        LEVEL_FRAGMENT = new NamespacedKey(plugin, "level_fragment");
        LEVEL_UPGRADER = new NamespacedKey(plugin, "level_upgrader");
    }
}
