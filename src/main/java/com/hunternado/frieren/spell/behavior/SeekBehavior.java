package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The spell to find something lost. Points to the caster's last death position in this dimension; otherwise to
 * the nearest unopened loot container (a chest that still has its loot table) or disguised mimic within
 * {@code search_radius} blocks. Only already-loaded chunks are inspected: no structure lookups, no chunk loading.
 */
public class SeekBehavior implements SpellBehavior {
    private static final int MAX_RADIUS = 96;

    @Override
    public boolean cast(SpellContext ctx) {
        MagicData data = ctx.data();
        BlockPos target;
        String key;
        if (data.lastDeathPos() != null && ctx.level().dimension().identifier().toString().equals(data.lastDeathDimension())) {
            target = data.lastDeathPos();
            key = "message.frieren.seek_death";
        } else {
            int radius = (int) Math.min(MAX_RADIUS, ctx.param("search_radius", 64.0D));
            target = nearestTreasure(ctx.level(), ctx.caster().blockPosition(), radius);
            key = "message.frieren.seek_treasure";
        }
        if (target == null) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.seek_nothing"));
            return false;
        }
        Vec3 from = ctx.caster().position();
        Vec3 delta = Vec3.atCenterOf(target).subtract(from);
        int distance = (int) Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        Messages.chat(ctx.caster(), Component.translatable(key, Component.translatable(compass(delta)), distance));
        Vec3 direction = new Vec3(delta.x, 0.0D, delta.z).normalize();
        SpellFx.beam(ctx.level(), from.add(0, 1.2D, 0), from.add(0, 1.2D, 0).add(direction.scale(6.0D)), ctx.color());
        SpellFx.sound(ctx.level(), from, ModSounds.UTILITY.get(), 0.8F, 0.9F);
        return true;
    }

    /** Nearest unlooted container or disguised mimic within {@code radius} blocks, in loaded chunks only. */
    private static @Nullable BlockPos nearestTreasure(ServerLevel level, BlockPos origin, int radius) {
        BlockPos best = null;
        double bestDistance = (double) radius * radius;
        int chunkRadius = (radius >> 4) + 1;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        for (int cx = originChunkX - chunkRadius; cx <= originChunkX + chunkRadius; cx++) {
            for (int cz = originChunkZ - chunkRadius; cz <= originChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof RandomizableContainer container && container.getLootTable() != null) {
                        double d = blockEntity.getBlockPos().distSqr(origin);
                        if (d < bestDistance) {
                            bestDistance = d;
                            best = blockEntity.getBlockPos();
                        }
                    }
                }
            }
        }
        for (Entity entity : level.getEntities((Entity) null, new AABB(origin).inflate(radius),
                e -> e instanceof MimicEntity mimic && !mimic.isRevealed())) {
            double d = entity.blockPosition().distSqr(origin);
            if (d < bestDistance) {
                bestDistance = d;
                best = entity.blockPosition();
            }
        }
        return best;
    }

    private static String compass(Vec3 delta) {
        double angle = Math.toDegrees(Math.atan2(delta.x, -delta.z));
        String[] names = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        int index = (int) Math.round(((angle % 360) + 360) % 360 / 45.0D) % 8;
        return "direction.frieren." + names[index];
    }
}
