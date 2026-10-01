package com.empoweredsmp.commands;

import com.empoweredsmp.managers.AbilityManager;
import com.empoweredsmp.model.PlayerData;
import com.empoweredsmp.util.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /curse <player>
 * Prosperity Main Ability. Curses an online player for 2 hours (Weakness I,
 * Slowness I, Mining Fatigue I and 9 hearts). Cooldown: 5 hours at Low Tier,
 * 3 hours at High Tier. Both timers use real time and survive restarts and
 * logouts; EffectManager keeps re-applying the curse so milk can't remove it.
 */
public class CurseCommand implements CommandExecutor {

    private static final long HOUR_MILLIS = 3_600_000L;

    private final AbilityManager abilities;
    private final Config cfg;

    public CurseCommand(AbilityManager abilities, Config cfg) {
        this.abilities = abilities;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!abilities.isProsperityAtLeast(p, 1)) {
            p.sendMessage(Component.text("Only Prosperity players (Low Tier or higher) can curse others.",
                    NamedTextColor.RED));
            return true;
        }
        if (args.length != 1) {
            p.sendMessage(Component.text("Usage: /curse <player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            p.sendMessage(Component.text("Player not found or not online: " + args[0], NamedTextColor.RED));
            return true;
        }
        if (target.equals(p)) {
            p.sendMessage(Component.text("You can't curse yourself.", NamedTextColor.RED));
            return true;
        }

        long now = System.currentTimeMillis();
        PlayerData mine = abilities.get(p);
        if (mine.getCurseCooldownUntil() > now) {
            p.sendMessage(Component.text("Your curse is on cooldown for another "
                    + formatDuration(mine.getCurseCooldownUntil() - now) + ".", NamedTextColor.RED));
            return true;
        }

        PlayerData theirs = abilities.get(target);
        if (theirs.isCursed(now)) {
            p.sendMessage(Component.text(target.getName() + " is already cursed.", NamedTextColor.RED));
            return true;
        }

        int level = abilities.levelOf(p);
        theirs.setCursedUntil(now + cfg.curseDurationHours() * HOUR_MILLIS);
        mine.setCurseCooldownUntil(now + cfg.curseCooldownHours(level) * HOUR_MILLIS);
        abilities.save(target);
        abilities.save(p);

        p.sendMessage(Component.text("You cursed " + target.getName() + " for " + cfg.curseDurationHours()
                + " hours. You can curse again in " + cfg.curseCooldownHours(level) + " hours.",
                NamedTextColor.DARK_PURPLE));
        target.sendMessage(Component.text("You have been cursed by " + p.getName() + " for "
                + cfg.curseDurationHours() + " hours!", NamedTextColor.DARK_PURPLE));
        return true;
    }

    private String formatDuration(long millis) {
        long totalMinutes = Math.max(1, millis / 60_000L);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
}
