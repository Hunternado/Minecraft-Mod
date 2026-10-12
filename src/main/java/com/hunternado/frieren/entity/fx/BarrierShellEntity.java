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

import java.util.List;

/**
 * The hexagonal shell of Defensive Magic flashing around its owner when it absorbs a hit. Repeated hits refresh
 * the existing shell instead of stacking new entities.
 */
public class BarrierShellEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(BarrierShellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(BarrierShellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(BarrierShellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_EXPIRES = SynchedEntityData.defineId(BarrierShellEntity.class, EntityDataSerializers.INT);

    public BarrierShellEntity(EntityType<? extends BarrierShellEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void flash(ServerLevel level, Entity owner, int color, int life) {
        float radius = Math.max(owner.getBbWidth(), owner.getBbHeight()) * 0.75F + 0.2F;
        AABB around = owner.getBoundingBox().inflate(2.0D);
        List<BarrierShellEntity> existing = level.getEntitiesOfClass(BarrierShellEntity.class, around,
            shell -> shell.ownerId() == owner.getId());
        if (!existing.isEmpty()) {
            BarrierShellEntity shell = existing.get(0);
            shell.entityData.set(DATA_EXPIRES, shell.tickCount + life);
            shell.entityData.set(DATA_COLOR, color);
            return;
        }
        BarrierShellEntity shell = ModEntities.BARRIER_SHELL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (shell == null) {
            return;
        }
        shell.entityData.set(DATA_OWNER, owner.getId());
        shell.entityData.set(DATA_COLOR, color);
        shell.entityData.set(DATA_RADIUS, radius);
        shell.entityData.set(DATA_EXPIRES, life);
        shell.setPos(owner.getX(), owner.getY() + owner.getBbHeight() / 2.0D, owner.getZ());
        level.addFreshEntity(shell);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_COLOR, 0x9FD8FF);
        builder.define(DATA_RADIUS, 1.4F);
        builder.define(DATA_EXPIRES, 10);
    }

    public int ownerId() {
        return entityData.get(DATA_OWNER);
    }

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public float radius() {
        return entityData.get(DATA_RADIUS);
    }

    public int expires() {
        return entityData.get(DATA_EXPIRES);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.level().getEntity(ownerId());
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() / 2.0D, owner.getZ());
        }
        if (this.level() instanceof ServerLevel && (this.tickCount >= expires() || owner == null || !owner.isAlive())) {
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
