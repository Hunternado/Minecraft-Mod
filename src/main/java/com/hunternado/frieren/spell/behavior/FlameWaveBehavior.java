package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.WorldRules;
import net.minecraft.core.BlockPos;
import com.hunternado.frieren.registry.ModSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Vollzanbel, the hellfire summoning spell: a cone of fire. Params: {@code damage}, {@code range},
 * {@code angle}, {@code burn_seconds}. Only places fire blocks when spell griefing is allowed.
 */
public class FlameWaveBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 8.0D);
        double angle = ctx.param("angle", 35.0D);
        float damage = ctx.scaled("damage", 7.0D);
        float burn = (float) ctx.param("burn_seconds", 5.0D);

        for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), ctx.caster().getEyePosition(), range,
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target) && SpellTargeting.inCone(ctx.caster(), target, angle))) {
            if (SpellDamage.hurt(ctx, target, damage, ModDamageTypes.SPELL, 1.0F)) {
                target.igniteForSeconds(burn);
            }
        }

        if (WorldRules.spellGriefing(ctx.level())) {
            Vec3 look = ctx.caster().getLookAngle();
            for (int i = 2; i <= (int) range; i += 2) {
                BlockPos pos = BlockPos.containing(ctx.caster().position().add(look.x * i, 0.0D, look.z * i));
                if (ctx.level().isEmptyBlock(pos) && !ctx.level().isEmptyBlock(pos.below())) {
                    ctx.level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }

        SpellFx.cone(ctx.level(), ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), ctx.color(), (float) range, 0);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.HELLFIRE.get(), 1.0F, 0.7F);
        return true;
    }
}
