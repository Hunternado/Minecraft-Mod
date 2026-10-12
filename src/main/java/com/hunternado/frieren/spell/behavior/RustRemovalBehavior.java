package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.statue.HeroStatueEntity;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The spell to remove rust from bronze statues — how Frieren keeps the Hero's statue clean. Reverses one
 * oxidation stage on copper blocks around the aimed block. Params: {@code range}, {@code radius}.
 */
public class RustRemovalBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        // The Hero's statue first: polish whichever statue the caster is looking at.
        double range = ctx.param("range", 6.0D);
        HeroStatueEntity statue = aimedStatue(ctx, range + 4.0D);
        if (statue != null) {
            int polished = statue.polish(ctx.caster());
            if (polished == 0) {
                Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.statue_already_clean"));
                return false;
            }
            ctx.markHit();
            for (int i = 0; i < polished; i++) {
                ctx.data().incrementStat("copper_polished");
            }
            QuestManager.onStat(ctx.caster(), ctx.data(), "copper_polished");
            return true;
        }
        BlockHitResult hit = SpellTargeting.clipBlocks(ctx.caster(), range);
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

    /** The statue whose bounding box the caster's line of sight crosses first, if any, before blocks get in the way. */
    private static @Nullable HeroStatueEntity aimedStatue(SpellContext ctx, double range) {
        Vec3 from = ctx.caster().getEyePosition();
        BlockHitResult blocks = SpellTargeting.clipBlocks(ctx.caster(), range);
        Vec3 to = blocks.getType() == HitResult.Type.MISS ? from.add(ctx.caster().getLookAngle().scale(range)) : blocks.getLocation();
        HeroStatueEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (HeroStatueEntity statue : ctx.level().getEntitiesOfClass(HeroStatueEntity.class, new AABB(from, to).inflate(1.0D))) {
            Optional<Vec3> clip = statue.getBoundingBox().inflate(0.3D).clip(from, to);
            if (clip.isPresent() && clip.get().distanceToSqr(from) < bestDistance) {
                bestDistance = clip.get().distanceToSqr(from);
                best = statue;
            }
        }
        return best;
    }
}
