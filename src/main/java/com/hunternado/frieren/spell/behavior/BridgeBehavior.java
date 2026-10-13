package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModBlocks;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The spell to lay a bridge of light: a level walkway of {@link com.hunternado.frieren.block.ManaBridgeBlock} planks
 * runs from under the caster's feet in the direction they face, over gaps, ravines and lava. It only fills air (water
 * is left alone, so no source block is ever lost) and fades after a minute. Params: {@code length}, {@code width}.
 */
public class BridgeBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        ServerLevel level = ctx.level();
        int length = (int) ctx.param("length", 16.0D);
        int half = Math.max(0, ((int) ctx.param("width", 3.0D) - 1) / 2);
        Direction facing = ctx.caster().getDirection();
        Direction across = facing.getClockWise();
        BlockPos start = ctx.caster().getOnPos();
        BlockState plank = ModBlocks.MANA_BRIDGE.get().defaultBlockState();
        int laid = 0;
        for (int step = 0; step <= length; step++) {
            BlockPos middle = start.relative(facing, step);
            for (int a = -half; a <= half; a++) {
                BlockPos pos = middle.relative(across, a);
                if (level.getBlockState(pos).isAir() && level.mayInteract(ctx.caster(), pos) && level.setBlock(pos, plank, 3)) {
                    laid++;
                }
            }
        }
        if (laid == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.bridge_nothing"));
            return false;
        }
        Vec3 from = Vec3.atCenterOf(start).add(0.0D, 0.6D, 0.0D);
        Vec3 to = Vec3.atCenterOf(start.relative(facing, length)).add(0.0D, 0.6D, 0.0D);
        SpellFx.beam(level, from, to, ctx.color(), 0.25F, 14, SpellBeamEntity.STYLE_LIGHT);
        SpellFx.sound(level, ctx.caster().position(), ModSounds.BARRIER_UP.get(), 0.8F, 1.4F);
        return true;
    }
}
