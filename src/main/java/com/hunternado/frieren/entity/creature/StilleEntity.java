package com.hunternado.frieren.entity.creature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The Stille: a tiny bird with absurd reflexes and mana resistance (First-Class exam, stage one). It darts
 * away from anyone nearby; three hits exhaust it for a few seconds, which is the only time a Stille Cage
 * can catch it. It never dies to players.
 */
public class StilleEntity extends PathfinderMob {
    private static final int LIFETIME = 6000;
    private static final int EXHAUST_TICKS = 160;

    private @Nullable UUID examinee;
    private int hits;
    private int exhaustedTicks;
    private int retargetDelay;
    private Vec3 flightTarget = Vec3.ZERO;
    private int age;

    public StilleEntity(EntityType<? extends StilleEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 4.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FLYING_SPEED, 0.6D);
    }

    public void setExaminee(UUID examinee) {
        this.examinee = examinee;
    }

    public boolean mayBeCapturedBy(Player player) {
        return examinee == null || examinee.equals(player.getUUID());
    }

    public boolean isExhausted() {
        return exhaustedTicks > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (++age > LIFETIME) {
            this.discard();
            return;
        }
        if (exhaustedTicks > 0) {
            exhaustedTicks--;
            this.setNoGravity(false);
            if (exhaustedTicks == 0) {
                hits = 0;
                this.setNoGravity(true);
            }
            return;
        }
        Player threat = level.getNearestPlayer(this, 8.0D);
        if (--retargetDelay <= 0 || this.position().distanceToSqr(flightTarget) < 1.0D || threat != null) {
            retargetDelay = 30 + this.getRandom().nextInt(30);
            Vec3 base = this.position();
            if (threat != null) {
                Vec3 away = base.subtract(threat.position()).normalize().scale(7.0D);
                flightTarget = base.add(away).add((this.getRandom().nextDouble() - 0.5D) * 4.0D, 1.0D + this.getRandom().nextDouble() * 2.0D,
                    (this.getRandom().nextDouble() - 0.5D) * 4.0D);
            } else {
                flightTarget = base.add((this.getRandom().nextDouble() - 0.5D) * 10.0D, (this.getRandom().nextDouble() - 0.4D) * 4.0D,
                    (this.getRandom().nextDouble() - 0.5D) * 10.0D);
            }
        }
        Vec3 delta = flightTarget.subtract(this.position());
        double speed = threat != null ? 0.55D : 0.25D;
        Vec3 velocity = delta.lengthSqr() > 1.0E-4D ? delta.normalize().scale(speed) : Vec3.ZERO;
        this.setDeltaMovement(velocity);
        if (velocity.lengthSqr() > 1.0E-4D) {
            float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) {
            if (++hits >= 3) {
                exhaustedTicks = EXHAUST_TICKS;
            }
            // Absurd mana resistance: players can tire it out, never kill it.
            return super.hurtServer(level, source, 0.0F);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Age", age);
        if (examinee != null) {
            output.putString("Examinee", examinee.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("Age", 0);
        String id = input.getStringOr("Examinee", "");
        try {
            examinee = id.isEmpty() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            examinee = null;
        }
    }
}
