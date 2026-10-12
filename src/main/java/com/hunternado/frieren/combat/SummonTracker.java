package com.hunternado.frieren.combat;

import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary summons (Serie's bottled golem). Each carries a {@code frieren_summon_until.<gameTime>} tag so the
 * expiry survives saving: running summons are discarded by the server tick, and expired ones loaded from disk
 * later are refused on load (see {@link #isExpired}).
 */
public final class SummonTracker {
    private static final String PREFIX = "frieren_summon_until.";
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();

    private SummonTracker() {}

    public static void track(ServerLevel level, Mob summon, int ticks) {
        long until = level.getGameTime() + ticks;
        summon.addTag(PREFIX + until);
        summon.setPersistenceRequired();
        ACTIVE.put(summon.getUUID(), until);
    }

    /** Twice a second: crumble summons whose time is up. */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            Iterator<Map.Entry<UUID, Long>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                Entity entity = level.getEntity(entry.getKey());
                if (entity == null) {
                    continue;
                }
                if (!entity.isAlive()) {
                    it.remove();
                } else if (now >= entry.getValue()) {
                    SpellFx.burst(level, entity.position().add(0.0D, entity.getBbHeight() / 2.0D, 0.0D), 0xE8E0C8, 1.2F);
                    entity.discard();
                    it.remove();
                }
            }
        }
    }

    /** A summon loaded from disk whose time ran out while it was unloaded. */
    public static boolean isExpired(Entity entity, long gameTime) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(PREFIX)) {
                try {
                    long until = Long.parseLong(tag.substring(PREFIX.length()));
                    if (gameTime >= until) {
                        return true;
                    }
                    ACTIVE.put(entity.getUUID(), until);
                } catch (NumberFormatException ignored) {
                    return true;
                }
            }
        }
        return false;
    }
}
