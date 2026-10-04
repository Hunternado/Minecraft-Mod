package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModBlocks;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Conjures a floating light where you look (or above your head); it fades after a few minutes. */
public class LightOrbBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        BlockHitResult hit = SpellTargeting.clipBlocks(ctx.caster(), ctx.param("range", 16.0D));
        BlockPos pos = hit.getType() == HitResult.Type.BLOCK
            ? hit.getBlockPos().relative(hit.getDirection())
            : ctx.caster().blockPosition().above(2);
        if (!ctx.level().getBlockState(pos).canBeReplaced()) {
            return false;
        }
        ctx.level().setBlock(pos, ModBlocks.MANA_LIGHT.get().defaultBlockState(), 3);
        SpellFx.burst(ctx.level(), Vec3.atCenterOf(pos), ctx.color(), 0.5F);
        SpellFx.sound(ctx.level(), Vec3.atCenterOf(pos), ModSounds.UTILITY.get(), 0.6F, 1.8F);
        return true;
    }
}
