package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The spell to fell a tree in one stroke: every log connected to the one you look at comes down, as if you had cut
 * each yourself (block protection, break events and tool wear apply). Only living trees answer: the logs must touch
 * natural leaves (or nether wart caps), so a log cabin is never felled by accident. Params: {@code range},
 * {@code max_logs}.
 */
public class TreeFellBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        ServerLevel level = ctx.level();
        BlockHitResult hit = SpellTargeting.clipBlocks(ctx.caster(), ctx.param("range", 6.0D));
        if (hit.getType() != HitResult.Type.BLOCK || !level.getBlockState(hit.getBlockPos()).is(BlockTags.LOGS)) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.fell_no_tree"));
            return false;
        }
        int max = (int) ctx.param("max_logs", 96.0D);
        List<BlockPos> logs = new ArrayList<>();
        boolean living = false;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(hit.getBlockPos());
        seen.add(hit.getBlockPos());
        while (!queue.isEmpty() && logs.size() < max) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            // big trees join their logs diagonally, so walk the whole 3x3x3 neighbourhood
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (!seen.add(next)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(next);
                        if (state.is(BlockTags.LOGS)) {
                            queue.add(next);
                        } else if (isNaturalCanopy(state)) {
                            living = true;
                        }
                    }
                }
            }
        }
        if (!living) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.fell_not_living"));
            return false;
        }
        int felled = 0;
        for (BlockPos pos : logs) {
            if (level.getBlockState(pos).is(BlockTags.LOGS) && ctx.caster().gameMode.destroyBlock(pos)) {
                felled++;
            }
        }
        if (felled == 0) {
            return false;
        }
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.fell_done", felled));
        SpellFx.streaks(level, Vec3.atCenterOf(hit.getBlockPos()), ctx.color(), 1.5F);
        SpellFx.sound(level, ctx.caster().position(), ModSounds.SLASH.get(), 0.8F, 0.8F);
        return true;
    }

    private static boolean isNaturalCanopy(BlockState state) {
        if (state.is(BlockTags.LEAVES)) {
            return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
        }
        return state.is(BlockTags.WART_BLOCKS);
    }
}
