package com.empoweredsmp;

import com.empoweredsmp.commands.ClassCommand;
import com.empoweredsmp.commands.CurseCommand;
import com.empoweredsmp.commands.EmpoweredAdminCommand;
import com.empoweredsmp.commands.ExtraInventoryCommand;
import com.empoweredsmp.commands.ToolChoiceCommand;
import com.empoweredsmp.commands.UnlockCommand;
import com.empoweredsmp.data.DataManager;
import com.empoweredsmp.data.GlobalState;
import com.empoweredsmp.listeners.CombatListener;
import com.empoweredsmp.listeners.DeathListener;
import com.empoweredsmp.listeners.ElementalAbilityListener;
import com.empoweredsmp.listeners.EnderPearlListener;
import com.empoweredsmp.listeners.EnforcementListener;
import com.empoweredsmp.listeners.ExtraInventoryListener;
import com.empoweredsmp.listeners.FragmentInteractListener;
import com.empoweredsmp.listeners.LungeListener;
import com.empoweredsmp.listeners.MainAbilityListener;
import com.empoweredsmp.listeners.TradePricingListener;
import com.empoweredsmp.listeners.VillagerListener;
import com.empoweredsmp.listeners.WorldAccessListener;
import com.empoweredsmp.listeners.XpListener;
import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.managers.EffectManager;
import com.empoweredsmp.managers.EnemyTracker;
import com.empoweredsmp.managers.ExtraInventoryManager;
import com.empoweredsmp.managers.RecipeRegistrar;
import com.empoweredsmp.managers.StarterKitManager;
import com.empoweredsmp.util.Config;
import com.empoweredsmp.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class EmpoweredSMP extends JavaPlugin {

    /** Base materials for the two custom-tagged items. Change here to use different vanilla items. */
    public static final Material LEVEL_FRAGMENT_MATERIAL = Material.PRISMARINE_SHARD;
    public static final Material LEVEL_UPGRADER_MATERIAL = Material.NETHER_STAR;

    /** Bump this whenever config.yml changes meaning, so old configs get replaced (and backed up). */
    private static final int CONFIG_VERSION = 2;

    private DataManager dataManager;
    private GlobalState globalState;
    private AbilityManager abilityManager;
    private CooldownManager cooldownManager;
    private ExtraInventoryManager extraInventoryManager;
    private EnemyTracker enemyTracker;
    private EffectManager effectManager;
    private Config pConfig;

    @Override
    public void onEnable() {
        ensureCurrentConfig();
        Keys.init(this);

        pConfig = new Config(getConfig());
        dataManager = new DataManager(this);
        globalState = new GlobalState(this);
        abilityManager = new AbilityManager(dataManager);
        cooldownManager = new CooldownManager();
        extraInventoryManager = new ExtraInventoryManager(dataManager, abilityManager, pConfig);
        enemyTracker = new EnemyTracker(pConfig);
        StarterKitManager starterKitManager = new StarterKitManager();

        // Recipe
        new RecipeRegistrar(this, LEVEL_FRAGMENT_MATERIAL, LEVEL_UPGRADER_MATERIAL).register();

        // Listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new DeathListener(abilityManager, dataManager, extraInventoryManager,
                LEVEL_FRAGMENT_MATERIAL), this);
        pm.registerEvents(new FragmentInteractListener(abilityManager, dataManager), this);
        pm.registerEvents(new EnforcementListener(this, abilityManager, pConfig), this);
        pm.registerEvents(new MainAbilityListener(this, abilityManager, cooldownManager, enemyTracker, pConfig), this);
        pm.registerEvents(new CombatListener(this, abilityManager, cooldownManager, enemyTracker, pConfig), this);
        pm.registerEvents(new VillagerListener(abilityManager, globalState), this);
        pm.registerEvents(new TradePricingListener(abilityManager, pConfig), this);
        pm.registerEvents(new WorldAccessListener(abilityManager, globalState), this);
        pm.registerEvents(new EnderPearlListener(cooldownManager, pConfig), this);
        pm.registerEvents(new LungeListener(cooldownManager, pConfig), this);
        pm.registerEvents(new ExtraInventoryListener(extraInventoryManager), this);
        pm.registerEvents(new XpListener(abilityManager, pConfig), this);
        pm.registerEvents(new ElementalAbilityListener(abilityManager), this);

        // Commands
        getCommand("class").setExecutor(new ClassCommand(abilityManager, starterKitManager));
        getCommand("ei").setExecutor(new ExtraInventoryCommand(extraInventoryManager));
        getCommand("empowered").setExecutor(new EmpoweredAdminCommand(abilityManager, dataManager));
        getCommand("toolchoice").setExecutor(new ToolChoiceCommand(abilityManager));
        getCommand("curse").setExecutor(new CurseCommand(abilityManager, pConfig));
        UnlockCommand unlock = new UnlockCommand(globalState);
        getCommand("unlocknether").setExecutor(unlock);
        getCommand("unlockend").setExecutor(unlock);
        getCommand("unlockvillagers").setExecutor(unlock);

        // Passive effect ticking
        effectManager = new EffectManager(this, abilityManager, pConfig);
        effectManager.start();

        getLogger().info("EmpoweredSMP enabled.");
    }

    /**
     * saveDefaultConfig() never overwrites an existing file, so a config.yml left over from
     * an older version would silently keep old numbers. If it's older than CONFIG_VERSION,
     * back it up as config-old.yml and write the fresh default.
     */
    private void ensureCurrentConfig() {
        saveDefaultConfig();
        if (getConfig().getInt("config-version", 0) >= CONFIG_VERSION) return;

        File current = new File(getDataFolder(), "config.yml");
        File backup = new File(getDataFolder(), "config-old.yml");
        if (backup.exists()) backup.delete();
        if (current.renameTo(backup)) {
            getLogger().warning("config.yml was from an older version. Saved it as config-old.yml "
                    + "and wrote a fresh config.yml.");
        }
        saveResource("config.yml", true);
        reloadConfig();
    }

    @Override
    public void onDisable() {
        if (effectManager != null) {
            effectManager.cancel();
        }
        if (dataManager != null) {
            dataManager.saveAll();
        }
        HandlerList.unregisterAll(this);
        getLogger().info("EmpoweredSMP disabled.");
    }

    public DataManager getDataManager() { return dataManager; }
    public GlobalState getGlobalState() { return globalState; }
    public AbilityManager getAbilityManager() { return abilityManager; }
}
