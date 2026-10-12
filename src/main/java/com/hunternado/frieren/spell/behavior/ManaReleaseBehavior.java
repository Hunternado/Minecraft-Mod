package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Mana Release: drop every bit of restraint and let a share of your remaining mana explode outward. The more mana
 * you pour in, the bigger and harder the blast. Params: {@code fraction} (share of current mana spent),
 * {@code min_mana}, {@code base_damage}, {@code damage_per_mana}, {@code base_radius}, {@code radius_per_mana},
 * {@code max_radius}.
 */
public class ManaReleaseBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        float spent = ctx.data().mana() * (float) ctx.param("fraction", 0.5D);
        if (spent < ctx.param("min_mana", 15.0D) || !ctx.data().trySpendMana(spent)) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.release_too_little"));
            return false;
        }
        ManaHelper.onManaSpent(ctx.caster(), ctx.data(), spent);
        ctx.data().pauseRegenUntil(ctx.level().getGameTime() + 60L);
        float damage = (float) ((ctx.param("base_damage", 4.0D) + spent * ctx.param("damage_per_mana", 0.12D)) * ctx.power());
        float radius = (float) Math.min(ctx.param("max_radius", 9.0D), ctx.param("base_radius", 3.5D) + spent * ctx.param("radius_per_mana", 0.03D));
        Vec3 center = ctx.caster().position();
        for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), center.add(0.0D, 1.0D, 0.0D), radius,
            candidate -> SpellDamage.canHarm(ctx.level(), ctx.caster(), candidate))) {
            if (SpellDamage.hurt(ctx, target, damage, ModDamageTypes.SPELL, 2.0F)) {
                Vec3 push = target.position().subtract(center).multiply(1.0D, 0.0D, 1.0D).normalize().scale(1.2D);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.5D, push.z));
                target.needsSync = true;
            }
        }
        MagicCircleEntity.onGround(ctx.level(), center, MagicCircleEntity.STYLE_ARCANE, ctx.color(), radius, 24);
        SpellFx.ring(ctx.level(), center.add(0.0D, 0.3D, 0.0D), ctx.color(), radius);
        SpellFx.burst(ctx.level(), center.add(0.0D, 1.0D, 0.0D), ctx.color(), radius * 0.6F);
        SpellFx.sound(ctx.level(), center, ModSounds.MANA_RELEASE.get(), 1.4F, 0.6F);
        return true;
    }
}
