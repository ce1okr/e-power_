package com.empoweredsmp.managers;

import com.empoweredsmp.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An "enemy" is a player who damaged you recently (enemies.memory-seconds) and
 * is still near you (enemies.radius, same world). Main Abilities that hit
 * "your enemies" use this.
 */
public class EnemyTracker {

    private final Config cfg;
    /** victim -> (attacker -> time of that attacker's last hit on the victim) */
    private final Map<UUID, Map<UUID, Long>> lastHit = new HashMap<>();

    public EnemyTracker(Config cfg) {
        this.cfg = cfg;
    }

    /** The player behind a damage source: the damager itself, or the shooter of a projectile. */
    public static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    public void recordHit(Player victim, Player attacker) {
        lastHit.computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>())
                .put(attacker.getUniqueId(), System.currentTimeMillis());
    }

    public List<Player> enemiesOf(Player victim) {
        Map<UUID, Long> hits = lastHit.get(victim.getUniqueId());
        if (hits == null) return List.of();

        long cutoff = System.currentTimeMillis() - cfg.enemyMemorySeconds() * 1000L;
        double radiusSquared = (double) cfg.enemyRadius() * cfg.enemyRadius();
        List<Player> result = new ArrayList<>();

        Iterator<Map.Entry<UUID, Long>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (entry.getValue() < cutoff) {
                it.remove();
                continue;
            }
            Player enemy = Bukkit.getPlayer(entry.getKey());
            if (enemy == null || !enemy.isOnline() || enemy.isDead()) continue;
            if (enemy.getWorld() != victim.getWorld()) continue;
            if (enemy.getLocation().distanceSquared(victim.getLocation()) > radiusSquared) continue;
            result.add(enemy);
        }
        return result;
    }
}
