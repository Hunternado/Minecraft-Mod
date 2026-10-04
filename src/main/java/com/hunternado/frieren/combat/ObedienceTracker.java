package com.hunternado.frieren.combat;

import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Hostility;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mobs bound by Azeryuze. Kept server-side only (not persisted: a restart releases puppets, which is
 * acceptable for a 60-second effect). Processed every 10 ticks.
 */
public final class ObedienceTracker {
    private record Entry(ResourceKey<Level> dimension, UUID owner, long expiresAt) {}

    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    private ObedienceTracker() {}

    public static void enthrall(ServerLevel level, Mob mob, Player owner, int duration) {
        ENTRIES.put(mob.getUUID(), new Entry(level.dimension(), owner.getUUID(), level.getGameTime() + duration));
        mob.setTarget(null);
        mob.setPersistenceRequired();
    }

    public static boolean isObedient(Entity entity) {
        return ENTRIES.containsKey(entity.getUUID());
    }

    public static @Nullable UUID ownerOf(Entity entity) {
        Entry entry = ENTRIES.get(entity.getUUID());
        return entry != null ? entry.owner() : null;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void tick(MinecraftServer server) {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Entry>> iterator = ENTRIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Entry> mapEntry = iterator.next();
            Entry entry = mapEntry.getValue();
            ServerLevel level = server.getLevel(entry.dimension());
            Entity entity = level != null ? level.getEntity(mapEntry.getKey()) : null;
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() >= entry.expiresAt()) {
                // Like Aura's thralls, the puppet does not survive the end of the spell.
                SpellFx.burst(level, mob.position().add(0, mob.getBbHeight() * 0.5D, 0), 0x7A1E2C, 1.0F);
                mob.discard();
                iterator.remove();
                continue;
            }
            LivingEntity target = mob.getTarget();
            if (target == null || target instanceof Player || isObedient(target)) {
                List<LivingEntity> enemies = SpellTargeting.livingInRadius(level, mob, mob.position(), 16.0D,
                    candidate -> Hostility.isHostile(candidate) && !isObedient(candidate));
                LivingEntity nearest = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity enemy : enemies) {
                    double distance = enemy.distanceToSqr(mob);
                    if (distance < best) {
                        best = distance;
                        nearest = enemy;
                    }
                }
                mob.setTarget(nearest);
            }
        }
    }
}
