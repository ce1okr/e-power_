package com.empoweredsmp.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Tracks the three one-way world gates. Once true, a flag is never set back
 * to false (enforced by only ever calling the unlock* setters, never a
 * lock* counterpart).
 */
public class GlobalState {

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    private boolean netherUnlocked;
    private boolean endUnlocked;
    private boolean villagersUnlocked;

    public GlobalState(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "global-state.yml");
        if (!file.exists()) {
            netherUnlocked = plugin.getConfig().getBoolean("global-gates.nether-unlocked", false);
            endUnlocked = plugin.getConfig().getBoolean("global-gates.end-unlocked", false);
            villagersUnlocked = plugin.getConfig().getBoolean("global-gates.villagers-unlocked", false);
            this.yaml = new YamlConfiguration();
            save();
        } else {
            this.yaml = YamlConfiguration.loadConfiguration(file);
            netherUnlocked = yaml.getBoolean("nether-unlocked", false);
            endUnlocked = yaml.getBoolean("end-unlocked", false);
            villagersUnlocked = yaml.getBoolean("villagers-unlocked", false);
        }
    }

    public boolean isNetherUnlocked() { return netherUnlocked; }
    public boolean isEndUnlocked() { return endUnlocked; }
    public boolean isVillagersUnlocked() { return villagersUnlocked; }

    /** @return true if this call actually changed state (was previously locked) */
    public boolean unlockNether() {
        if (netherUnlocked) return false;
        netherUnlocked = true;
        save();
        return true;
    }

    public boolean unlockEnd() {
        if (endUnlocked) return false;
        endUnlocked = true;
        save();
        return true;
    }

    public boolean unlockVillagers() {
        if (villagersUnlocked) return false;
        villagersUnlocked = true;
        save();
        return true;
    }

    private void save() {
        yaml.set("nether-unlocked", netherUnlocked);
        yaml.set("end-unlocked", endUnlocked);
        yaml.set("villagers-unlocked", villagersUnlocked);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save global-state.yml", e);
        }
    }
}
