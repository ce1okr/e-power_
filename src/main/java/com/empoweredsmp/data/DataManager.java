package com.empoweredsmp.data;

import com.empoweredsmp.model.Ability;
import com.empoweredsmp.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persists each player's ability, tier, death counter, curse timers and
 * Prosperity extra-inventory contents to individual YAML files under
 * plugins/EmpoweredSMP/playerdata/<uuid>.yml.
 */
public class DataManager {

    private final Plugin plugin;
    private final File folder;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) folder.mkdirs();
    }

    private File fileFor(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        File f = fileFor(uuid);
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            data.setAbility(Ability.fromString(y.getString("ability", null)));
            // setLevel clamps to 0..2, so old Level 2-3 players load as High Tier.
            data.setLevel(y.getInt("level", 0));
            data.setDeathCounter(y.getInt("death-counter", 0));
            String tool1Str = y.getString("prosperity-tool-1", null);
            String tool2Str = y.getString("prosperity-tool-2", null);
            if (tool1Str != null && tool2Str != null) {
                try {
                    data.setProsperityTools(Material.valueOf(tool1Str), Material.valueOf(tool2Str));
                } catch (IllegalArgumentException ignored) {
                    // stored material name no longer valid; leave unset
                }
            }
            data.setCurseCooldownUntil(y.getLong("curse-cooldown-until", 0L));
            data.setCursedUntil(y.getLong("cursed-until", 0L));
        }
        return data;
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        YamlConfiguration y = new YamlConfiguration();
        y.set("ability", data.getAbility() == null ? null : data.getAbility().name());
        y.set("level", data.getLevel());
        y.set("death-counter", data.getDeathCounter());
        if (data.hasChosenProsperityTools()) {
            y.set("prosperity-tool-1", data.getProsperityTool1().name());
            y.set("prosperity-tool-2", data.getProsperityTool2().name());
        }
        y.set("curse-cooldown-until", data.getCurseCooldownUntil());
        y.set("cursed-until", data.getCursedUntil());
        try {
            y.save(fileFor(uuid));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save playerdata for " + uuid, e);
        }
    }

    public void saveAll() {
        for (UUID u : cache.keySet()) save(u);
    }

    // --- Extra inventory (Prosperity only) persistence, stored in a sibling file ---

    private File extraInvFileFor(UUID uuid) {
        return new File(folder, uuid.toString() + "-extrainv.yml");
    }

    public void loadExtraInventory(UUID uuid, Inventory inv) {
        File f = extraInvFileFor(uuid);
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        Object raw = y.get("contents");
        if (raw instanceof ItemStack[] items) {
            for (int i = 0; i < Math.min(items.length, inv.getSize()); i++) {
                inv.setItem(i, items[i]);
            }
        }
    }

    public void saveExtraInventory(UUID uuid, Inventory inv) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("contents", inv.getContents());
        try {
            y.save(extraInvFileFor(uuid));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save extra inventory for " + uuid, e);
        }
    }

    public void clearExtraInventoryFile(UUID uuid) {
        File f = extraInvFileFor(uuid);
        if (f.exists()) f.delete();
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }
}
