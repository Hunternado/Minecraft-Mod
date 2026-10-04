package com.hunternado.frieren.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Raycasts used by spells. Implemented with plain AABB/clip math so it does not depend on vanilla
 * projectile helpers.
 */
public final class SpellTargeting {
    private SpellTargeting() {}

    public record EntityHit(LivingEntity entity, Vec3 location, double distance) {}

    /** Where the caster is looking, stopped by solid blocks. */
    public static BlockHitResult clipBlocks(Entity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        return caster.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
    }

    /** End point of the caster's line of sight: block hit position or max range. */
    public static Vec3 aimPoint(Entity caster, double range) {
        BlockHitResult hit = clipBlocks(caster, range);
        return hit.getType() == HitResult.Type.MISS ? caster.getEyePosition().add(caster.getLookAngle().scale(range)) : hit.getLocation();
    }

    /**
     * All living entities intersecting the segment from {@code from} to {@code to} (inflated by
     * {@code thickness}), nearest first.
     */
    public static List<EntityHit> entitiesOnLine(ServerLevel level, Entity caster, Vec3 from, Vec3 to, double thickness,
                                                 Predicate<LivingEntity> filter) {
        AABB search = new AABB(from, to).inflate(thickness + 1.0D);
        List<EntityHit> hits = new ArrayList<>();
        for (Entity entity : level.getEntities(caster, search, e -> e instanceof LivingEntity living && living.isAlive())) {
            LivingEntity living = (LivingEntity) entity;
            if (!filter.test(living)) {
                continue;
            }
            Optional<Vec3> clip = living.getBoundingBox().inflate(thickness).clip(from, to);
            if (clip.isPresent()) {
                hits.add(new EntityHit(living, clip.get(), from.distanceTo(clip.get())));
            } else if (living.getBoundingBox().inflate(thickness).contains(from)) {
                hits.add(new EntityHit(living, from, 0.0D));
            }
        }
        hits.sort(Comparator.comparingDouble(EntityHit::distance));
        return hits;
    }

    /** First living entity along the caster's line of sight (blocks stop the ray). */
    public static @Nullable EntityHit firstEntity(ServerLevel level, Entity caster, double range, Predicate<LivingEntity> filter) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = aimPoint(caster, range);
        List<EntityHit> hits = entitiesOnLine(level, caster, eye, end, 0.3D, filter);
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** Living entities within {@code radius} of {@code center}. */
    public static List<LivingEntity> livingInRadius(ServerLevel level, @Nullable Entity except, Vec3 center, double radius,
                                                    Predicate<LivingEntity> filter) {
        AABB box = new AABB(center, center).inflate(radius);
        double radiusSqr = radius * radius;
        List<LivingEntity> result = new ArrayList<>();
        for (Entity entity : level.getEntities(except, box, e -> e instanceof LivingEntity living && living.isAlive())) {
            LivingEntity living = (LivingEntity) entity;
            if (living.position().distanceToSqr(center) <= radiusSqr && filter.test(living)) {
                result.add(living);
            }
        }
        return result;
    }

    /** True if {@code target} is inside a cone of {@code halfAngleDegrees} in front of the caster. */
    public static boolean inCone(Entity caster, Entity target, double halfAngleDegrees) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(caster.getEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6D) {
            return true;
        }
        double cos = look.dot(toTarget.normalize());
        return cos >= Math.cos(Math.toRadians(halfAngleDegrees));
    }
}
