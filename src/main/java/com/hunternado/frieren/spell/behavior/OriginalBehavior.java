package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.boss.DemonBoss;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.OriginalSpells;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The single behavior behind every spell Serie creates; what it does is read from the params that
 * {@link OriginalSpells#build} baked in ({@code form}, {@code element}, {@code damage}, {@code range}, {@code radius},
 * {@code count}, {@code duration}, {@code heal}, {@code absorb}, {@code boss_fraction}, {@code pierce},
 * {@code modifiers}). Damage goes through {@link SpellDamage}, so executions, friendly fire and barriers work as for
 * any other spell.
 */
public class OriginalBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int form = (int) ctx.param("form", 0.0D);
        boolean done = switch (form) {
            case OriginalSpells.FORM_VOLLEY -> volley(ctx);
            case OriginalSpells.FORM_BLAST -> blast(ctx);
            case OriginalSpells.FORM_NOVA -> nova(ctx);
            case OriginalSpells.FORM_WARD -> ward(ctx);
            case OriginalSpells.FORM_MEND -> mend(ctx);
            case OriginalSpells.FORM_BIND -> bind(ctx);
            default -> beam(ctx);
        };
        if (done) {
            boolean annihilation = ctx.param("boss_fraction", 0.0D) > 0.0D;
            SpellFx.sound(ctx.level(), ctx.caster().position(), annihilation ? ModSounds.ANNIHILATION.get() : ModSounds.ZOLTRAAK.get(),
                annihilation ? 1.6F : 0.9F, annihilation ? 0.7F : 1.1F);
        }
        return done;
    }

    // ---- Forms -------------------------------------------------------------------------------

    private static boolean beam(SpellContext ctx) {
        Vec3 from = ctx.caster().getEyePosition();
        Vec3 direction = ctx.caster().getLookAngle();
        double range = ctx.param("range", 32.0D);
        Vec3 wanted = from.add(direction.scale(range));
        BlockHitResult blockHit = ctx.level().clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster()));
        Vec3 to = blockHit.getType() == HitResult.Type.MISS ? wanted : blockHit.getLocation();
        boolean annihilation = ctx.param("boss_fraction", 0.0D) > 0.0D;
        double thickness = annihilation ? 0.9D : 0.3D;
        List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(ctx.level(), ctx.caster(), from, to, thickness,
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target));
        boolean pierce = annihilation || ctx.param("pierce", 0.0D) > 0.5D;
        float damage = ctx.scaled("damage", 6.0D);
        Vec3 end = to;
        Set<LivingEntity> struck = new HashSet<>();
        for (SpellTargeting.EntityHit hit : hits) {
            strike(ctx, hit.entity(), damage);
            struck.add(hit.entity());
            if (!pierce) {
                end = hit.location();
                break;
            }
        }
        float width = annihilation ? 1.6F * (0.4F + 0.6F * ctx.charge()) : 0.35F;
        SpellFx.beam(ctx.level(), from.add(direction.scale(0.6D)), end, ctx.color(), width, 10 + (int) (width * 8.0F),
            element(ctx) == OriginalSpells.EL_STORM ? SpellBeamEntity.STYLE_LIGHTNING : SpellBeamEntity.STYLE_ZOLTRAAK);
        SpellFx.burst(ctx.level(), end, ctx.color(), Math.max(0.6F, width * 1.8F));
        if (!struck.isEmpty() && element(ctx) == OriginalSpells.EL_STORM) {
            arc(ctx, struck.iterator().next(), struck, (int) ctx.param("count", 1.0D) - 1, damage * 0.6F);
        }
        return true;
    }

    /** Bolts of light that each seek a different foe in front of the caster (or fly straight when there are none). */
    private static boolean volley(SpellContext ctx) {
        int count = Math.max(1, (int) ctx.param("count", 4.0D));
        double range = ctx.param("range", 32.0D);
        Vec3 eye = ctx.caster().getEyePosition();
        List<LivingEntity> foes = new ArrayList<>(SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), eye, range,
            target -> SpellDamage.canHarm(ctx.level(), ctx.caster(), target) && SpellTargeting.inCone(ctx.caster(), target, 40.0D)
                && ctx.caster().hasLineOfSight(target)));
        foes.sort(Comparator.comparingDouble(target -> target.distanceToSqr(ctx.caster())));
        float damage = ctx.scaled("damage", 3.0D);
        for (int i = 0; i < count; i++) {
            Vec3 from = eye.add(ctx.caster().getLookAngle().yRot((float) Math.toRadians((i - (count - 1) / 2.0D) * 14.0D)).scale(0.9D));
            if (!foes.isEmpty()) {
                LivingEntity target = foes.get(i % foes.size());
                Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
                SpellFx.beam(ctx.level(), from, at, ctx.color(), 0.18F, 10, SpellBeamEntity.STYLE_LIGHT);
                strike(ctx, target, damage);
            } else {
                Vec3 at = SpellTargeting.aimPoint(ctx.caster(), range);
                SpellFx.beam(ctx.level(), from, at, ctx.color(), 0.18F, 10, SpellBeamEntity.STYLE_LIGHT);
            }
        }
        return true;
    }

    private static boolean blast(SpellContext ctx) {
        Vec3 at = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 32.0D));
        double radius = ctx.param("radius", 3.0D);
        area(ctx, at, radius, false);
        MagicCircleEntity.onGround(ctx.level(), at, MagicCircleEntity.STYLE_ARCANE, ctx.color(), (float) radius, 16);
        SpellFx.beam(ctx.level(), at.add(0.0D, 10.0D, 0.0D), at, ctx.color(), 0.5F + (float) radius * 0.1F, 12, SpellBeamEntity.STYLE_LIGHT);
        return true;
    }

    private static boolean nova(SpellContext ctx) {
        area(ctx, ctx.caster().position(), ctx.param("radius", 4.0D), true);
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0.0D, 0.2D, 0.0D), ctx.color(), (float) ctx.param("radius", 4.0D));
        return true;
    }

    private static boolean ward(SpellContext ctx) {
        LivingEntity self = ctx.caster();
        int duration = Math.round(ctx.scaled("duration", 300.0D));
        int hearts = Math.max(0, Math.round(ctx.scaled("absorb", 8.0D) / 4.0F) - 1);
        self.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, hearts));
        switch (element(ctx)) {
            case OriginalSpells.EL_FLAME -> self.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
            case OriginalSpells.EL_HOLY -> self.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 2, 1));
            case OriginalSpells.EL_VOID -> self.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.REFLECTING), Math.max(40, duration / 4), 0));
            case OriginalSpells.EL_STORM -> self.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1));
            case OriginalSpells.EL_CURSE -> self.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 0));
            default -> self.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0));
        }
        SpellFx.shell(ctx.level(), self, ctx.color());
        ctx.markHit();
        return true;
    }

    private static boolean mend(SpellContext ctx) {
        double radius = ctx.param("radius", 4.0D);
        float heal = ctx.scaled("heal", 8.0D);
        boolean holy = element(ctx) == OriginalSpells.EL_HOLY;
        List<LivingEntity> allies = SpellTargeting.livingInRadius(ctx.level(), null, ctx.caster().position(), radius,
            target -> target == ctx.caster() || HealBehavior.isFriendly(target));
        for (LivingEntity ally : allies) {
            ally.heal(heal);
            if (holy) {
                // Goddess-grade mending: every curse and poison washes away.
                List<MobEffectInstance> harmful = new ArrayList<>();
                for (MobEffectInstance effect : ally.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        harmful.add(effect);
                    }
                }
                for (MobEffectInstance effect : harmful) {
                    ally.removeEffect(effect.getEffect());
                }
            }
            SpellFx.burst(ctx.level(), ally.position().add(0.0D, ally.getBbHeight() * 0.6D, 0.0D), ctx.color(), 0.7F);
            ctx.markHit();
        }
        SpellFx.petals(ctx.level(), ctx.caster().position(), ctx.color(), (float) radius);
        return true;
    }

    private static boolean bind(SpellContext ctx) {
        Vec3 at = SpellTargeting.aimPoint(ctx.caster(), ctx.param("range", 24.0D));
        double radius = ctx.param("radius", 3.0D);
        int duration = Math.round(ctx.scaled("duration", 60.0D));
        float damage = ctx.scaled("damage", 2.0D);
        for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), at, radius,
            candidate -> SpellDamage.canHarm(ctx.level(), ctx.caster(), candidate))) {
            int ticks = target instanceof FrierenBoss ? duration / 4 : duration;
            target.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.BOUND), ticks, 0));
            strike(ctx, target, damage);
        }
        MagicCircleEntity.onGround(ctx.level(), at, MagicCircleEntity.STYLE_ARCANE, ctx.color(), (float) radius, Math.min(80, duration));
        SpellFx.ring(ctx.level(), at.add(0.0D, 0.2D, 0.0D), ctx.color(), (float) radius);
        return true;
    }

    // ---- Shared ------------------------------------------------------------------------------

    private static void area(SpellContext ctx, Vec3 center, double radius, boolean pushOut) {
        float damage = ctx.scaled("damage", 6.0D) * (element(ctx) == OriginalSpells.EL_STORM ? 1.25F : 1.0F);
        for (LivingEntity target : SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), center.add(0.0D, 0.5D, 0.0D), radius,
            candidate -> SpellDamage.canHarm(ctx.level(), ctx.caster(), candidate))) {
            strike(ctx, target, damage);
            if (pushOut) {
                Vec3 away = target.position().subtract(center).multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.9D).add(0.0D, 0.35D, 0.0D);
                target.setDeltaMovement(target.getDeltaMovement().add(away));
                target.needsSync = true;
            }
        }
        SpellFx.burst(ctx.level(), center.add(0.0D, 0.5D, 0.0D), ctx.color(), (float) radius);
    }

    /** One hit with the spell's element: holy doubles against demons and the undead, Annihilation breaks bosses. */
    private static void strike(SpellContext ctx, LivingEntity target, float amount) {
        int element = element(ctx);
        float damage = amount;
        if (element == OriginalSpells.EL_HOLY && (target instanceof DemonEntity || target instanceof DemonBoss || target.isInvertedHealAndHarm())) {
            damage *= 2.0F;
        }
        float bossFraction = (float) ctx.param("boss_fraction", 0.0D) * ctx.charge();
        if (bossFraction > 0.0F && (target instanceof FrierenBoss || target.is(SpellDamage.EXECUTE_IMMUNE))) {
            damage = Math.max(damage, target.getMaxHealth() * bossFraction);
        }
        float before = target.getHealth();
        ResourceKey<DamageType> type = element == OriginalSpells.EL_CURSE ? ModDamageTypes.CURSE : ModDamageTypes.SPELL;
        if (!SpellDamage.hurt(ctx, target, damage, type, (float) ctx.param("barrier_cost", 1.0D))) {
            return;
        }
        int potency = (int) ctx.param("potency", 1.0D);
        switch (element) {
            case OriginalSpells.EL_FLAME -> target.igniteForSeconds(2.0F + potency);
            case OriginalSpells.EL_FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 + 20 * potency, Math.min(3, potency / 2)));
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 20 * potency);
                SpellFx.frost(ctx.level(), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), ctx.color(), 0.8F);
            }
            case OriginalSpells.EL_CURSE -> {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60 + 20 * potency, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100 + 20 * potency, 0));
            }
            default -> {
            }
        }
        if (((int) ctx.param("modifiers", 0.0D) & OriginalSpells.MOD_LIFESTEAL) != 0) {
            float dealt = Math.max(0.0F, before - Math.max(0.0F, target.getHealth()));
            ctx.caster().heal(dealt * 0.25F);
        }
    }

    /** Storm: the bolt leaps on to the nearest foes the first strike has not hit yet. */
    private static void arc(SpellContext ctx, LivingEntity from, Set<LivingEntity> struck, int jumps, float damage) {
        LivingEntity current = from;
        for (int i = 0; i < jumps; i++) {
            LivingEntity source = current;
            LivingEntity next = SpellTargeting.livingInRadius(ctx.level(), ctx.caster(), source.position(), 7.0D,
                    candidate -> !struck.contains(candidate) && SpellDamage.canHarm(ctx.level(), ctx.caster(), candidate))
                .stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(source))).orElse(null);
            if (next == null) {
                return;
            }
            SpellFx.beam(ctx.level(), source.position().add(0.0D, source.getBbHeight() * 0.6D, 0.0D),
                next.position().add(0.0D, next.getBbHeight() * 0.6D, 0.0D), ctx.color(), 0.2F, 8, SpellBeamEntity.STYLE_LIGHTNING);
            strike(ctx, next, damage);
            struck.add(next);
            current = next;
        }
    }

    private static int element(SpellContext ctx) {
        return (int) ctx.param("element", 0.0D);
    }
}
