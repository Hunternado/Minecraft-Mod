package com.hunternado.frieren.entity.magic;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Magic missile used by demon mages, Qual and Fern's NPC. A plain entity with explicit straight-line
 * movement and collision so it does not depend on vanilla projectile internals; visuals are client-side
 * particles.
 */
public class ManaBoltEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(ManaBoltEntity.class, EntityDataSerializers.INT);
    private static final int MAX_LIFE = 80;

    private @Nullable UUID ownerId;
    private @Nullable Entity cachedOwner;
    private float damage = 4.0F;
    private float barrierCost = 1.0F;
    private ResourceKey<DamageType> damageType = ModDamageTypes.DEMON_MAGIC;
    private int life;

    public ManaBoltEntity(EntityType<? extends ManaBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static @Nullable ManaBoltEntity shoot(ServerLevel level, LivingEntity shooter, Vec3 target, float damage, float speed, int color,
                                                 ResourceKey<DamageType> type, float barrierCost) {
        ManaBoltEntity bolt = ModEntities.MANA_BOLT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return null;
        }
        Vec3 start = shooter.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
        Vec3 velocity = target.subtract(start).normalize().scale(speed);
        bolt.setPos(start);
        bolt.setDeltaMovement(velocity);
        bolt.ownerId = shooter.getUUID();
        bolt.cachedOwner = shooter;
        bolt.damage = damage;
        bolt.barrierCost = barrierCost;
        bolt.damageType = type;
        bolt.entityData.set(DATA_COLOR, color);
        level.addFreshEntity(bolt);
        return bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, 0xC03A5A);
    }

    public int color() {
        return this.entityData.get(DATA_COLOR);
    }

    private @Nullable Entity owner(ServerLevel level) {
        if (cachedOwner == null && ownerId != null) {
            cachedOwner = level.getEntity(ownerId);
        }
        return cachedOwner;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 to = from.add(motion);

        if (this.level() instanceof ServerLevel level) {
            if (++life > MAX_LIFE) {
                this.discard();
                return;
            }
            BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 end = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
            Entity owner = owner(level);
            Entity attacker = owner != null ? owner : this;
            List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(level, this, from, end, 0.3D,
                target -> target != owner && SpellDamage.canHarm(level, attacker, target));
            if (!hits.isEmpty()) {
                SpellDamage.hurt(level, this, attacker, hits.get(0).entity(), damage, damageType, barrierCost, false, null);
                SpellFx.burst(level, hits.get(0).location(), color(), 0.5F);
                this.discard();
                return;
            }
            if (blockHit.getType() != HitResult.Type.MISS) {
                SpellFx.burst(level, end, color(), 0.4F);
                this.discard();
                return;
            }
        } else {
            for (int i = 0; i < 3; i++) {
                double t = i / 3.0D;
                this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(255, color())),
                    from.x + motion.x * t, from.y + motion.y * t, from.z + motion.z * t, 0.0D, 0.0D, 0.0D);
            }
            this.level().addParticle(ParticleTypes.END_ROD, from.x, from.y, from.z, 0.0D, 0.0D, 0.0D);
        }
        this.setPos(to);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Damage", damage);
        output.putFloat("BarrierCost", barrierCost);
        output.putInt("Life", life);
        output.putInt("Color", color());
        output.putString("DamageType", damageType.identifier().toString());
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        damage = input.getFloatOr("Damage", 4.0F);
        barrierCost = input.getFloatOr("BarrierCost", 1.0F);
        life = input.getIntOr("Life", 0);
        this.entityData.set(DATA_COLOR, input.getIntOr("Color", 0xC03A5A));
        Identifier typeId = Identifier.tryParse(input.getStringOr("DamageType", ""));
        if (typeId != null) {
            damageType = ResourceKey.create(Registries.DAMAGE_TYPE, typeId);
        }
        String owner = input.getStringOr("Owner", "");
        try {
            ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            ownerId = null;
        }
    }
}
