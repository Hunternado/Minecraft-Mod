package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sorganeil: whatever you keep in your gaze cannot move. Channelled: the target stays bound while the caster
 * holds the cast and keeps looking at it; breaking line of sight (or looking away) ends the spell. Bosses shrug it
 * off after {@code boss_ticks}. Params: {@code range}, {@code pulse_cost} (mana per second), {@code gaze_angle},
 * {@code boss_ticks}.
 */
public class BindBehavior implements SpellBehavior {
    /** Caster -> bound entity id for the running channel (one channel per caster at a time). */
    private static final Map<UUID, Integer> TARGETS = new HashMap<>();

    @Override
    public boolean cast(SpellContext ctx) {
        double range = ctx.param("range", 24.0D);
        SpellTargeting.EntityHit hit = SpellTargeting.firstEntity(ctx.level(), ctx.caster(), range,
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target));
        if (hit == null) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.no_target"));
            return false;
        }
        LivingEntity target = hit.entity();
        TARGETS.put(ctx.caster().getUUID(), target.getId());
        bind(target);
        ctx.markHit();
        MagicCircleEntity.onGround(ctx.level(), target.position(), MagicCircleEntity.STYLE_ARCANE, ctx.color(),
            Math.max(0.8F, target.getBbWidth()), 40);
        SpellFx.streaks(ctx.level(), target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D), ctx.color(), 1.0F);
        SpellFx.sound(ctx.level(), target.position(), ModSounds.CURSE.get(), 0.8F, 1.6F);
        return true;
    }

    @Override
    public boolean tickChannel(SpellContext ctx, int channelTick) {
        Integer id = TARGETS.get(ctx.caster().getUUID());
        Entity entity = id != null ? ctx.level().getEntity(id) : null;
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            TARGETS.remove(ctx.caster().getUUID());
            return false;
        }
        double range = ctx.param("range", 24.0D);
        Vec3 eye = ctx.caster().getEyePosition();
        Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D).subtract(eye);
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, toTarget.normalize().dot(ctx.caster().getLookAngle())))));
        boolean bossFree = target instanceof FrierenBoss && channelTick > (int) ctx.param("boss_ticks", 30.0D);
        if (toTarget.length() > range + 4.0D || angle > ctx.param("gaze_angle", 15.0D) || !ctx.caster().hasLineOfSight(target) || bossFree) {
            TARGETS.remove(ctx.caster().getUUID());
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.bind_broken"));
            return false;
        }
        if (channelTick % 20 == 0 && !ctx.spendExtraMana((float) ctx.param("pulse_cost", 3.0D))) {
            TARGETS.remove(ctx.caster().getUUID());
            return false;
        }
        bind(target);
        if (channelTick % 10 == 0) {
            SpellFx.streaks(ctx.level(), target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D), ctx.color(), 0.7F);
        }
        return true;
    }

    private static void bind(LivingEntity target) {
        target.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.BOUND), 8, 0, false, false, true));
        target.setDeltaMovement(0.0D, Math.min(0.0D, target.getDeltaMovement().y), 0.0D);
        target.needsSync = true;
    }
}
