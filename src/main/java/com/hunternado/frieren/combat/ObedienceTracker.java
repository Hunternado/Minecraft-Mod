package com.hunternado.frieren.combat;

import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Ground;
import com.hunternado.frieren.util.Hostility;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mobs bound by Azeryuze, and the orders their master gives them with the Command Puppets key:
 * <ul>
 *   <li>look at a creature: every puppet turns on it (ATTACK) until it dies;</li>
 *   <li>look at a block: they go there and hold it, fighting anything hostile that comes close (GUARD);</li>
 *   <li>look at nothing: they fall in behind you and fight whatever threatens you (FOLLOW, the default);</li>
 *   <li>sneak: "Die." They obey one last time and crumble (release).</li>
 * </ul>
 * A puppet never turns on any player or on another puppet (target changes are vetoed, see
 * {@link #vetoTarget}). Kept server-side only, not persisted: a restart releases puppets. Processed every 10 ticks.
 */
public final class ObedienceTracker {
    public enum Order { FOLLOW, GUARD, ATTACK }

    private static final double COMMAND_RANGE = 32.0D;
    private static final double DEFEND_RADIUS = 10.0D;

    private static final class Entry {
        final ResourceKey<Level> dimension;
        final UUID owner;
        final long expiresAt;
        Order order = Order.FOLLOW;
        @Nullable UUID target;
        @Nullable BlockPos guard;

        Entry(ResourceKey<Level> dimension, UUID owner, long expiresAt) {
            this.dimension = dimension;
            this.owner = owner;
            this.expiresAt = expiresAt;
        }
    }

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
        return entry != null ? entry.owner : null;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    /** LivingChangeTargetEvent listener: true cancels a puppet turning on a player or a fellow puppet. */
    public static boolean vetoTarget(LivingChangeTargetEvent event) {
        if (!ENTRIES.containsKey(event.getEntity().getUUID())) {
            return false;
        }
        LivingEntity target = event.getNewTarget();
        return target instanceof Player || (target != null && isObedient(target));
    }

    // ------------------------------------------------------------------------------------------
    // Orders
    // ------------------------------------------------------------------------------------------

    /** The Command Puppets key: what the master is looking at decides the order. */
    public static void command(ServerPlayer owner) {
        ServerLevel level = owner.level();
        List<Mob> puppets = puppetsOf(level, owner.getUUID());
        if (puppets.isEmpty()) {
            Messages.actionBar(owner, Component.translatable("message.frieren.puppets_none").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (owner.isShiftKeyDown()) {
            for (Mob puppet : puppets) {
                crumble(level, puppet);
                ENTRIES.remove(puppet.getUUID());
            }
            Messages.actionBar(owner, Component.translatable("message.frieren.puppets_release").withStyle(ChatFormatting.DARK_RED));
            return;
        }
        SpellTargeting.EntityHit hit = SpellTargeting.firstEntity(level, owner, COMMAND_RANGE,
            candidate -> !isObedient(candidate) && SpellDamage.canHarm(level, owner, candidate));
        if (hit != null) {
            for (Mob puppet : puppets) {
                Entry entry = ENTRIES.get(puppet.getUUID());
                entry.order = Order.ATTACK;
                entry.target = hit.entity().getUUID();
                puppet.setTarget(hit.entity());
            }
            SpellFx.burst(level, hit.entity().position().add(0, hit.entity().getBbHeight() + 0.4D, 0), 0xC8283A, 0.6F);
            Messages.actionBar(owner, Component.translatable("message.frieren.puppets_attack", hit.entity().getDisplayName())
                .withStyle(ChatFormatting.RED));
            return;
        }
        BlockHitResult block = SpellTargeting.clipBlocks(owner, COMMAND_RANGE);
        if (block.getType() == HitResult.Type.BLOCK) {
            BlockPos spot = block.getBlockPos().relative(block.getDirection());
            for (Mob puppet : puppets) {
                Entry entry = ENTRIES.get(puppet.getUUID());
                entry.order = Order.GUARD;
                entry.guard = spot;
                entry.target = null;
                puppet.setTarget(null);
                puppet.getNavigation().moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 1.15D);
            }
            SpellFx.ring(level, Vec3.atBottomCenterOf(spot).add(0, 0.1D, 0), 0xC8283A, 1.2F);
            Messages.actionBar(owner, Component.translatable("message.frieren.puppets_guard").withStyle(ChatFormatting.GOLD));
            return;
        }
        for (Mob puppet : puppets) {
            Entry entry = ENTRIES.get(puppet.getUUID());
            entry.order = Order.FOLLOW;
            entry.target = null;
            entry.guard = null;
            puppet.setTarget(null);
        }
        Messages.actionBar(owner, Component.translatable("message.frieren.puppets_follow").withStyle(ChatFormatting.AQUA));
    }

    private static List<Mob> puppetsOf(ServerLevel level, UUID owner) {
        List<Mob> out = new ArrayList<>();
        for (Map.Entry<UUID, Entry> e : ENTRIES.entrySet()) {
            if (e.getValue().owner.equals(owner) && e.getValue().dimension == level.dimension()
                    && level.getEntity(e.getKey()) instanceof Mob mob && mob.isAlive()) {
                out.add(mob);
            }
        }
        return out;
    }

    private static void crumble(ServerLevel level, Mob mob) {
        // Like Aura's thralls, the puppet does not outlive its orders.
        SpellFx.burst(level, mob.position().add(0, mob.getBbHeight() * 0.5D, 0), 0x7A1E2C, 1.0F);
        mob.discard();
    }

    // ------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Entry>> iterator = ENTRIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Entry> mapEntry = iterator.next();
            Entry entry = mapEntry.getValue();
            ServerLevel level = server.getLevel(entry.dimension);
            Entity entity = level != null ? level.getEntity(mapEntry.getKey()) : null;
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() >= entry.expiresAt) {
                crumble(level, mob);
                iterator.remove();
                continue;
            }
            switch (entry.order) {
                case ATTACK -> attack(level, mob, entry);
                case GUARD -> guard(level, mob, entry);
                case FOLLOW -> follow(level, mob, entry);
            }
        }
    }

    private static void attack(ServerLevel level, Mob mob, Entry entry) {
        Entity target = entry.target != null ? level.getEntity(entry.target) : null;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            entry.order = Order.FOLLOW;                       // the job is done: back to their master's side
            entry.target = null;
            mob.setTarget(null);
            return;
        }
        if (mob.getTarget() != living) {
            mob.setTarget(living);
        }
        if (!(mob instanceof Enemy)) {
            mob.getNavigation().moveTo(living, 1.2D);         // creatures without an attack still crowd the target
        }
    }

    private static void guard(ServerLevel level, Mob mob, Entry entry) {
        BlockPos spot = entry.guard != null ? entry.guard : mob.blockPosition();
        Vec3 centre = Vec3.atBottomCenterOf(spot);
        if (!validFoe(mob.getTarget()) || mob.getTarget().position().distanceToSqr(centre) > 14.0D * 14.0D) {
            mob.setTarget(nearestFoe(level, mob, centre));
        }
        if (mob.getTarget() == null && mob.position().distanceToSqr(centre) > 2.5D * 2.5D) {
            mob.getNavigation().moveTo(centre.x, centre.y, centre.z, 1.15D);
        }
    }

    private static void follow(ServerLevel level, Mob mob, Entry entry) {
        Player owner = level.getPlayerByUUID(entry.owner);
        if (owner == null || !owner.isAlive()) {
            if (!validFoe(mob.getTarget())) {
                mob.setTarget(nearestFoe(level, mob, mob.position()));
            }
            return;
        }
        if (!validFoe(mob.getTarget()) || mob.getTarget().distanceToSqr(owner) > 16.0D * 16.0D) {
            mob.setTarget(nearestFoe(level, mob, owner.position()));
        }
        double distance = mob.distanceToSqr(owner);
        if (distance > 32.0D * 32.0D) {
            Vec3 spot = Ground.standable(level, owner.position().subtract(owner.getLookAngle().multiply(2.0D, 0.0D, 2.0D)));
            if (spot != null) {
                mob.teleportTo(spot.x, spot.y, spot.z);
                mob.getNavigation().stop();
            }
        } else if (mob.getTarget() == null && distance > 4.5D * 4.5D) {
            mob.getNavigation().moveTo(owner, 1.25D);
        }
    }

    private static boolean validFoe(@Nullable LivingEntity entity) {
        return entity != null && entity.isAlive() && !(entity instanceof Player) && !isObedient(entity);
    }

    private static @Nullable LivingEntity nearestFoe(ServerLevel level, Mob mob, Vec3 around) {
        List<LivingEntity> enemies = SpellTargeting.livingInRadius(level, mob, around, DEFEND_RADIUS,
            candidate -> Hostility.isHostile(candidate) && validFoe(candidate));
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity enemy : enemies) {
            double distance = enemy.distanceToSqr(mob);
            if (distance < best) {
                best = distance;
                nearest = enemy;
            }
        }
        return nearest;
    }
}
