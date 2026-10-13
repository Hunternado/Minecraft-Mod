package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The spell to ripen the fields: crops, saplings, stems, berry bushes and cocoa around the caster grow as if
 * bone-mealed a few times over (grass and flowers are left alone, so lawns do not sprout a jungle). Params:
 * {@code radius}, {@code rounds}.
 */
public class GrowthBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int radius = (int) ctx.param("radius", 5.0D);
        int rounds = (int) ctx.param("rounds", 2.0D);
        BlockPos center = ctx.caster().blockPosition();
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockPos at = pos.immutable();
            boolean any = false;
            for (int i = 0; i < rounds; i++) {
                BlockState state = ctx.level().getBlockState(at);
                if (!isPlant(state) || !(state.getBlock() instanceof BonemealableBlock plant)
                        || !plant.isValidBonemealTarget(ctx.level(), at, state)) {
                    break;
                }
                if (plant.isBonemealSuccess(ctx.level(), ctx.level().getRandom(), at, state)) {
                    plant.performBonemeal(ctx.level(), ctx.level().getRandom(), at, state);
                    any = true;
                }
            }
            if (any) {
                grown++;
                SpellFx.burst(ctx.level(), Vec3.atCenterOf(at), ctx.color(), 0.4F);
            }
        }
        if (grown == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.growth_nothing"));
            return false;
        }
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.growth_done", grown));
        SpellFx.petals(ctx.level(), Vec3.atCenterOf(center), ctx.color(), radius);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.FLOWER_BLOOM.get(), 1.0F, 1.0F);
        return true;
    }

    private static boolean isPlant(BlockState state) {
        return state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.getBlock() instanceof StemBlock
            || state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof CocoaBlock;
    }
}
