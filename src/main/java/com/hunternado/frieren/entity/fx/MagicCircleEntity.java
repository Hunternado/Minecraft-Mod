package com.hunternado.frieren.entity.fx;

import com.hunternado.frieren.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A spell's magic circle: a short-lived visual entity drawn by MagicCircleRenderer. Either hangs in front of its
 * caster (following them while a spell winds up or channels) or lies flat on the ground under an area spell.
 * Never saved; it vanishes on reload.
 */
public class MagicCircleEntity extends Entity {
    public static final byte STYLE_ARCANE = 0;
    public static final byte STYLE_HOLY = 1;
    public static final byte STYLE_DEMON = 2;
    public static final byte STYLE_FLORA = 3;

    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_STYLE = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLAT = SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.BOOLEAN);

    private int followId = -1;
    private float distance;

    public MagicCircleEntity(EntityType<? extends MagicCircleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** A vertical circle in front of the caster, facing where they look; follows them for its lifetime. */
    public static void inFront(ServerLevel level, LivingEntity caster, byte style, int color, float radius, int life, float distance) {
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (circle == null) {
            return;
        }
        circle.setup(style, color, radius, life, false);
        circle.followId = caster.getId();
        circle.distance = distance;
        circle.anchorTo(caster);
        level.addFreshEntity(circle);
    }

    /** A free-standing vertical circle at a fixed position and orientation (bosses, set pieces). */
    public static void standing(ServerLevel level, Vec3 center, float yaw, float pitch, byte style, int color, float radius, int life) {
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (circle == null) {
            return;
        }
        circle.setup(style, color, radius, life, false);
        circle.setPos(center.x, center.y, center.z);
        circle.setYRot(yaw);
        circle.setXRot(pitch);
        level.addFreshEntity(circle);
    }

    /** A flat circle on the ground (area spells, summons, barriers). */
    public static @Nullable MagicCircleEntity onGround(ServerLevel level, Vec3 center, byte style, int color, float radius, int life) {
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (circle == null) {
            return null;
        }
        circle.setup(style, color, radius, life, true);
        circle.setPos(center.x, center.y + 0.05D, center.z);
        return level.addFreshEntity(circle) ? circle : null;
    }

    private void setup(byte style, int color, float radius, int life, boolean flat) {
        this.entityData.set(DATA_STYLE, style);
        this.entityData.set(DATA_COLOR, color);
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_FLAT, flat);
    }

    private void anchorTo(Entity caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 eye = caster.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
        this.setPos(eye.add(look.scale(distance)));
        this.setYRot(caster.getYRot());
        this.setXRot(caster.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, 0xA8D8FF);
        builder.define(DATA_RADIUS, 1.0F);
        builder.define(DATA_STYLE, STYLE_ARCANE);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_FLAT, false);
    }

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public float radius() {
        return entityData.get(DATA_RADIUS);
    }

    public byte style() {
        return entityData.get(DATA_STYLE);
    }

    public int life() {
        return entityData.get(DATA_LIFE);
    }

    public boolean flat() {
        return entityData.get(DATA_FLAT);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount >= life()) {
            this.discard();
            return;
        }
        if (followId >= 0) {
            Entity caster = level.getEntity(followId);
            if (caster == null || !caster.isAlive()) {
                this.discard();
                return;
            }
            // A caster who teleports (ender pearl, blink, /tp) leaves the circle behind instead of dragging it along.
            Vec3 before = this.position();
            anchorTo(caster);
            if (before.distanceToSqr(this.position()) > 16.0D) {
                this.discard();
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.discard();
    }
}
