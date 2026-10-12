package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The spell to harvest crops: every fully grown crop around the caster drops its harvest and is replanted on the
 * spot. Params: {@code radius}.
 */
public class HarvestBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int radius = (int) ctx.param("radius", 6.0D);
        BlockPos center = ctx.caster().blockPosition();
        int harvested = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockState state = ctx.level().getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                BlockPos immutable = pos.immutable();
                ctx.level().destroyBlock(immutable, true, ctx.caster());
                ctx.level().setBlock(immutable, crop.getStateForAge(0), 3);
                harvested++;
            }
        }
        if (harvested == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.harvest_nothing"));
            return false;
        }
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.harvest_done", harvested));
        SpellFx.petals(ctx.level(), Vec3.atCenterOf(center), ctx.color(), radius);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.FLOWER_BLOOM.get(), 1.0F, 1.2F);
        return true;
    }
}
