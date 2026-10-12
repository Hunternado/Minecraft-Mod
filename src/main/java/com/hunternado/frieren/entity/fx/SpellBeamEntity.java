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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The visible part of a beam spell (Zoltraak and friends): a bright core with a soft halo from the caster to the
 * impact point, drawn by SpellBeamRenderer for a few ticks. Purely visual; damage is resolved by the spell.
 */
public class SpellBeamEntity extends Entity {
    public static final byte STYLE_ZOLTRAAK = 0;
    public static final byte STYLE_DEMON = 1;
    public static final byte STYLE_LIGHT = 2;
    public static final byte STYLE_LIGHTNING = 3;

    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DX = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DY = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DZ = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_STYLE = SynchedEntityData.defineId(SpellBeamEntity.class, EntityDataSerializers.BYTE);

    public SpellBeamEntity(EntityType<? extends SpellBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void fire(ServerLevel level, Vec3 from, Vec3 to, int color, float width, int life, byte style) {
        SpellBeamEntity beam = ModEntities.SPELL_BEAM.get().create(level, EntitySpawnReason.TRIGGERED);
        if (beam == null) {
            return;
        }
        beam.setPos(from);
        Vec3 d = to.subtract(from);
        beam.entityData.set(DATA_DX, (float) d.x);
        beam.entityData.set(DATA_DY, (float) d.y);
        beam.entityData.set(DATA_DZ, (float) d.z);
        beam.entityData.set(DATA_COLOR, color);
        beam.entityData.set(DATA_WIDTH, width);
        beam.entityData.set(DATA_LIFE, life);
        beam.entityData.set(DATA_STYLE, style);
        level.addFreshEntity(beam);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, 0xFFFFFF);
        builder.define(DATA_DX, 0.0F);
        builder.define(DATA_DY, 0.0F);
        builder.define(DATA_DZ, 1.0F);
        builder.define(DATA_WIDTH, 0.25F);
        builder.define(DATA_LIFE, 10);
        builder.define(DATA_STYLE, STYLE_ZOLTRAAK);
    }

    public Vec3 offset() {
        return new Vec3(entityData.get(DATA_DX), entityData.get(DATA_DY), entityData.get(DATA_DZ));
    }

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public float width() {
        return entityData.get(DATA_WIDTH);
    }

    public int life() {
        return entityData.get(DATA_LIFE);
    }

    public byte style() {
        return entityData.get(DATA_STYLE);
    }

    /** Whole beam must stay visible even when its origin is off-screen. */
    public AABB beamBounds() {
        Vec3 a = this.position();
        Vec3 b = a.add(offset());
        return new AABB(a, b).inflate(1.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && this.tickCount >= life()) {
            this.discard();
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
