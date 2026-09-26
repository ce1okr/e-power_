package com.empoweredsmp;

import com.empoweredsmp.commands.ClassCommand;
import com.empoweredsmp.commands.ExtraInventoryCommand;
import com.empoweredsmp.commands.EmpoweredAdminCommand;
import com.empoweredsmp.commands.BowChoiceCommand;
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
import com.empoweredsmp.listeners.RangerCrossbowListener;
import com.empoweredsmp.listeners.SurgeListener;
import com.empoweredsmp.listeners.VillagerListener;
import com.empoweredsmp.listeners.WorldAccessListener;
import com.empoweredsmp.listeners.XpListener;
import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.managers.CooldownManager;
import com.empoweredsmp.managers.EffectManager;
import com.empoweredsmp.managers.ExtraInventoryManager;
import com.empoweredsmp.managers.RecipeRegistrar;
import com.empoweredsmp.managers.StarterKitManager;
import com.empoweredsmp.util.Keys;
import com.empoweredsmp.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class EmpoweredSMP extends JavaPlugin {

    /** Base materials used for the two custom-tagged items. Change here if you'd rather
     *  use different vanilla items (no resource pack needed either way). */
    public static final Material LEVEL_FRAGMENT_MATERIAL = Material.PRISMARINE_SHARD;
    public static final Material LEVEL_UPGRADER_MATERIAL = Material.NETHER_STAR;

    private DataManager dataManager;
    private GlobalState globalState;
    private AbilityManager abilityManager;
    private CooldownManager cooldownManager;
    private ExtraInventoryManager extraInventoryManager;
    private EffectManager effectManager;
    private Config pConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        pConfig = new Config(getConfig());
        dataManager = new DataManager(this);
        globalState = new GlobalState(this);
        abilityManager = new AbilityManager(dataManager);
        cooldownManager = new CooldownManager();
        extraInventoryManager = new ExtraInventoryManager(dataManager, abilityManager, pConfig);
        StarterKitManager starterKitManager = new StarterKitManager();

        // Recipe
        new RecipeRegistrar(this, LEVEL_FRAGMENT_MATERIAL, LEVEL_UPGRADER_MATERIAL).register();

        // Listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new DeathListener(abilityManager, dataManager, extraInventoryManager, LEVEL_FRAGMENT_MATERIAL), this);
        pm.registerEvents(new FragmentInteractListener(abilityManager, dataManager, starterKitManager), this);
        pm.registerEvents(new EnforcementListener(abilityManager, cooldownManager, pConfig), this);
        pm.registerEvents(new SurgeListener(this, abilityManager, cooldownManager, pConfig), this);
        pm.registerEvents(new CombatListener(abilityManager, pConfig), this);
        pm.registerEvents(new VillagerListener(abilityManager, globalState), this);
        pm.registerEvents(new WorldAccessListener(abilityManager, globalState), this);
        pm.registerEvents(new EnderPearlListener(cooldownManager, pConfig), this);
        pm.registerEvents(new LungeListener(cooldownManager), this);
        pm.registerEvents(new ExtraInventoryListener(extraInventoryManager), this);
        pm.registerEvents(new RangerCrossbowListener(), this);
        pm.registerEvents(new XpListener(abilityManager, pConfig), this);
        pm.registerEvents(new ElementalAbilityListener(abilityManager), this);

        // Commands
        getCommand("class").setExecutor(new ClassCommand(abilityManager, starterKitManager));
        getCommand("ei").setExecutor(new ExtraInventoryCommand(extraInventoryManager));
        getCommand("empowered").setExecutor(new EmpoweredAdminCommand(abilityManager, dataManager, starterKitManager));
        getCommand("bowchoice").setExecutor(new BowChoiceCommand(abilityManager));
        getCommand("toolchoice").setExecutor(new ToolChoiceCommand(abilityManager));
        UnlockCommand unlock = new UnlockCommand(globalState);
        getCommand("unlocknether").setExecutor(unlock);
        getCommand("unlockend").setExecutor(unlock);
        getCommand("unlockvillagers").setExecutor(unlock);

        // Passive effect ticking
        effectManager = new EffectManager(this, abilityManager, pConfig);
        effectManager.start();

        getLogger().info("EmpoweredSMP enabled.");
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
