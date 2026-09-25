package com.empoweredsmp.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Long> expiryMillis = new HashMap<>();

    private String key(UUID uuid, String tag) {
        return uuid + ":" + tag;
    }

    public boolean isOnCooldown(UUID uuid, String tag) {
        Long expiry = expiryMillis.get(key(uuid, tag));
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public long remainingSeconds(UUID uuid, String tag) {
        Long expiry = expiryMillis.get(key(uuid, tag));
        if (expiry == null) return 0;
        long remainMs = expiry - System.currentTimeMillis();
        return Math.max(0, remainMs / 1000);
    }

    public void set(UUID uuid, String tag, int seconds) {
        expiryMillis.put(key(uuid, tag), System.currentTimeMillis() + seconds * 1000L);
    }

    public void clear(UUID uuid, String tag) {
        expiryMillis.remove(key(uuid, tag));
    }
}
