package com.hunternado.frieren.entity.magic;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.demon.AuraThrallEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Hostility;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Stationary magic construct with a lifetime: Waldgose's tornado, Catastravia's light rain, the barrier
 * dome, the flower field's aura and Aura's guillotine telegraph. Server logic is throttled per mode;
 * visuals are client-side particles.
 */
public class SpellAreaEntity extends Entity {
    public enum Mode {
        TORNADO, LIGHT_RAIN, DOME, FLOWER_FIELD, GUILLOTINE_MARK;

        static Mode byId(int id) {
            Mode[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(SpellAreaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(SpellAreaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SpellAreaEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID ownerId;
    private @Nullable Entity cachedOwner;
    private int duration = 60;
    private float damage;
    private int age;

    public SpellAreaEntity(EntityType<? extends SpellAreaEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static @Nullable SpellAreaEntity spawn(ServerLevel level, @Nullable Entity owner, Vec3 at, Mode mode, float radius, int duration,
                                                  float damage, int color) {
        SpellAreaEntity area = ModEntities.SPELL_AREA.get().create(level, EntitySpawnReason.TRIGGERED);
        if (area == null) {
            return null;
        }
        area.setPos(at);
        area.ownerId = owner != null ? owner.getUUID() : null;
        area.cachedOwner = owner;
        area.duration = Math.max(1, duration);
        area.damage = damage;
        area.entityData.set(DATA_MODE, mode.ordinal());
        area.entityData.set(DATA_RADIUS, radius);
        area.entityData.set(DATA_COLOR, color);
        level.addFreshEntity(area);
        return area;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, 0);
        builder.define(DATA_RADIUS, 3.0F);
        builder.define(DATA_COLOR, 0xFFFFFF);
    }

    public Mode mode() {
        return Mode.byId(this.entityData.get(DATA_MODE));
    }

    public float radius() {
        return this.entityData.get(DATA_RADIUS);
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
        age++;
        if (this.level() instanceof ServerLevel level) {
            serverTick(level);
            if (age >= duration) {
                this.discard();
            }
        } else {
            clientTick();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Server
    // ---------------------------------------------------------------------------------------

    private void serverTick(ServerLevel level) {
        Entity owner = owner(level);
        Entity attacker = owner != null ? owner : this;
        float radius = radius();
        Vec3 center = this.position();
        switch (mode()) {
            case TORNADO -> {
                List<LivingEntity> caught = SpellTargeting.livingInRadius(level, owner, center, radius,
                    target -> SpellDamage.canHarm(level, attacker, target));
                for (LivingEntity target : caught) {
                    Vec3 pull = center.subtract(target.position());
                    Vec3 push = new Vec3(pull.x, 0.0D, pull.z).normalize().scale(0.12D).add(0.0D, 0.11D, 0.0D);
                    if (age >= duration - 1) {
                        push = new Vec3(-pull.x, 0.0D, -pull.z).normalize().scale(0.8D).add(0.0D, 0.9D, 0.0D);
                    }
                    target.setDeltaMovement(target.getDeltaMovement().add(push));
                    target.needsSync = true;
                    target.fallDistance = 0.0F;
                    if (age % 10 == 0) {
                        SpellDamage.hurt(level, this, attacker, target, damage, ModDamageTypes.SPELL, 1.0F, false, null);
                    }
                }
                if (age % 20 == 0) {
                    SpellFx.spiral(level, center, color(), 5.0F);
                }
            }
            case LIGHT_RAIN -> {
                if (age % 3 == 0) {
                    RandomSource random = level.getRandom();
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = Math.sqrt(random.nextDouble()) * radius;
                    Vec3 point = center.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
                    SpellFx.beam(level, point.add(0.0D, 12.0D, 0.0D), point, color());
                    for (LivingEntity target : SpellTargeting.livingInRadius(level, owner, point, 1.6D,
                        candidate -> SpellDamage.canHarm(level, attacker, candidate))) {
                        SpellDamage.hurt(level, this, attacker, target, damage, ModDamageTypes.SPELL, 1.0F, false, null);
                    }
                }
            }
            case DOME -> {
                if (age % 5 == 0) {
                    AABB box = new AABB(center, center).inflate(radius);
                    for (Entity entity : level.getEntities(this, box, e -> e instanceof Projectile)) {
                        Projectile projectile = (Projectile) entity;
                        if (projectile.getOwner() != null && Hostility.isHostile(projectile.getOwner())
                            && projectile.position().distanceToSqr(center) <= radius * radius) {
                            SpellFx.burst(level, projectile.position(), color(), 0.3F);
                            projectile.discard();
                        }
                    }
                    for (LivingEntity ally : SpellTargeting.livingInRadius(level, null, center, radius,
                        target -> target instanceof Player || !Hostility.isHostile(target))) {
                        ally.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.WARDED), 30, 0));
                        ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0));
                    }
                }
                if (age % 30 == 0) {
                    SpellFx.ring(level, center.add(0.0D, 0.1D, 0.0D), color(), radius);
                }
            }
            case FLOWER_FIELD -> {
                if (age % 40 == 0) {
                    for (LivingEntity ally : SpellTargeting.livingInRadius(level, null, center, radius,
                        target -> target instanceof Player || !Hostility.isHostile(target))) {
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                    }
                }
            }
            case GUILLOTINE_MARK -> {
                if (age == duration - 1) {
                    for (LivingEntity target : SpellTargeting.livingInRadius(level, owner, center, radius,
                        candidate -> candidate != owner && !(candidate instanceof AuraThrallEntity))) {
                        SpellDamage.hurt(level, this, attacker, target, damage, ModDamageTypes.GUILLOTINE, 1.5F, false, null);
                    }
                    SpellFx.burst(level, center.add(0.0D, 0.5D, 0.0D), color(), radius);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Client visuals
    // ---------------------------------------------------------------------------------------

    private void clientTick() {
        Level level = this.level();
        RandomSource random = level.getRandom();
        float radius = radius();
        Vec3 center = this.position();
        ColorParticleOption tint = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(255, color()));
        switch (mode()) {
            case TORNADO -> {
                for (int i = 0; i < 6; i++) {
                    double height = random.nextDouble() * 6.0D;
                    double angle = (age * 0.5D + height * 2.0D + i) % (Math.PI * 2.0D);
                    double r = 0.4D + height / 6.0D * radius;
                    level.addParticle(ParticleTypes.CLOUD, center.x + Math.cos(angle) * r, center.y + height, center.z + Math.sin(angle) * r,
                        0.0D, 0.05D, 0.0D);
                }
            }
            case LIGHT_RAIN -> {
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = Math.sqrt(random.nextDouble()) * radius;
                    level.addParticle(ParticleTypes.END_ROD, center.x + Math.cos(angle) * distance, center.y + 10.0D,
                        center.z + Math.sin(angle) * distance, 0.0D, -1.2D, 0.0D);
                }
            }
            case DOME -> {
                for (int i = 0; i < 4; i++) {
                    double theta = random.nextDouble() * Math.PI * 2.0D;
                    double phi = random.nextDouble() * Math.PI * 0.5D;
                    level.addParticle(tint, center.x + Math.cos(theta) * Math.cos(phi) * radius, center.y + Math.sin(phi) * radius,
                        center.z + Math.sin(theta) * Math.cos(phi) * radius, 0.0D, 0.0D, 0.0D);
                }
            }
            case FLOWER_FIELD -> {
                if (age % 4 == 0) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = random.nextDouble() * radius;
                    level.addParticle(ParticleTypes.HAPPY_VILLAGER, center.x + Math.cos(angle) * distance, center.y + 0.3D,
                        center.z + Math.sin(angle) * distance, 0.0D, 0.02D, 0.0D);
                }
            }
            case GUILLOTINE_MARK -> {
                for (int i = 0; i < 8; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    level.addParticle(tint, center.x + Math.cos(angle) * radius, center.y + 0.1D, center.z + Math.sin(angle) * radius, 0.0D, 0.02D, 0.0D);
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Mode", mode().ordinal());
        output.putFloat("Radius", radius());
        output.putInt("Color", color());
        output.putInt("Duration", duration);
        output.putInt("Age", age);
        output.putFloat("Damage", damage);
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_MODE, input.getIntOr("Mode", 0));
        this.entityData.set(DATA_RADIUS, input.getFloatOr("Radius", 3.0F));
        this.entityData.set(DATA_COLOR, input.getIntOr("Color", 0xFFFFFF));
        duration = input.getIntOr("Duration", 60);
        age = input.getIntOr("Age", 0);
        damage = input.getFloatOr("Damage", 0.0F);
        String owner = input.getStringOr("Owner", "");
        try {
            ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            ownerId = null;
        }
    }
}
