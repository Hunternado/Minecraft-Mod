package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The spell to remove rust from bronze statues — how Frieren keeps the Hero's statue clean. Reverses one
 * oxidation stage on copper blocks around the aimed block. Params: {@code range}, {@code radius}.
 */
public class RustRemovalBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        BlockHitResult hit = SpellTargeting.clipBlocks(ctx.caster(), ctx.param("range", 6.0D));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        int radius = (int) ctx.param("radius", 2.0D);
        BlockPos center = hit.getBlockPos();
        int polished = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState state = ctx.level().getBlockState(pos);
            Optional<BlockState> previous = WeatheringCopper.getPrevious(state);
            if (previous.isPresent()) {
                ctx.level().setBlock(pos, previous.get(), 3);
                polished++;
            }
        }
        if (polished == 0) {
            return false;
        }
        ctx.markHit();
        for (int i = 0; i < polished; i++) {
            ctx.data().incrementStat("copper_polished");
        }
        QuestManager.onStat(ctx.caster(), ctx.data(), "copper_polished");
        SpellFx.burst(ctx.level(), Vec3.atCenterOf(center), ctx.color(), radius + 0.5F);
        SpellFx.sound(ctx.level(), Vec3.atCenterOf(center), ModSounds.UTILITY.get(), 1.0F, 1.3F);
        return true;
    }
}
