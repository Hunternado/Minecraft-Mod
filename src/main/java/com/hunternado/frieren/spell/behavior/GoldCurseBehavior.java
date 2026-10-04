package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.registry.ModBlocks;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.WorldRules;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Diagolze, Macht's curse that turns things into gold. Living targets become gilded statues for a while
 * (bosses resist); with spell griefing enabled, stone becomes worthless Cursed Gold.
 * Params: {@code range}, {@code duration}, {@code damage}, {@code boss_duration}.
 */
public class GoldCurseBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 16.0D);
        SpellTargeting.EntityHit hit = SpellTargeting.firstEntity(ctx.level(), ctx.caster(), range,
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target));
        if (hit != null) {
            boolean boss = hit.entity() instanceof FrierenBoss;
            int duration = (int) ((boss ? ctx.param("boss_duration", 20.0D) : ctx.param("duration", 120.0D)) * ctx.power());
            hit.entity().addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.GILDED), duration, 0));
            SpellDamage.hurt(ctx, hit.entity(), ctx.scaled("damage", 4.0D), ModDamageTypes.CURSE, 2.0F);
            ctx.markHit();
            SpellFx.beam(ctx.level(), ctx.caster().getEyePosition(), hit.location(), ctx.color());
            SpellFx.burst(ctx.level(), hit.entity().position().add(0, hit.entity().getBbHeight() * 0.5D, 0), ctx.color(), 1.2F);
            SpellFx.sound(ctx.level(), hit.location(), ModSounds.CURSE.get(), 1.0F, 0.6F);
            return true;
        }

        BlockHitResult blockHit = SpellTargeting.clipBlocks(ctx.caster(), range);
        if (blockHit.getType() != HitResult.Type.BLOCK || !WorldRules.spellGriefing(ctx.level())) {
            return false;
        }
        int changed = 0;
        BlockPos center = blockHit.getBlockPos();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (ctx.level().getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                ctx.level().setBlock(pos, ModBlocks.CURSED_GOLD_BLOCK.get().defaultBlockState(), 3);
                changed++;
            }
        }
        if (changed > 0) {
            SpellFx.burst(ctx.level(), Vec3.atCenterOf(center), ctx.color(), 1.5F);
            SpellFx.sound(ctx.level(), Vec3.atCenterOf(center), ModSounds.CURSE.get(), 1.0F, 0.8F);
        }
        return changed > 0;
    }
}
