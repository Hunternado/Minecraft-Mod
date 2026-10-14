package com.hunternado.frieren.entity.creature;

import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * away from anyone nearby; three hits (melee, arrows or spells) exhaust it for eight seconds, which is the only
 * time a Stille Cage can catch it. It never dies to players. Every step is spelled out to whoever is chasing it:
 * a countdown of hits, a loud "exhausted" with sweat drops while it rests, and a notice when it takes off again.
 */
public class StilleEntity extends PathfinderMob {
    private static final int LIFETIME = 6000;
    private static final int EXHAUST_TICKS = 160;
    private static final int HITS_TO_EXHAUST = 3;

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

    /** Hits still needed before it tires out (shown to the player chasing it). */
    public int hitsToExhaust() {
        return Math.max(0, HITS_TO_EXHAUST - hits);
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
            if (exhaustedTicks % 10 == 0) {                    // sweat drops while it pants on the ground
                level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.5D, this.getZ(), 4, 0.15D, 0.1D, 0.15D, 0.0D);
            }
            if (exhaustedTicks == 0) {
                hits = 0;
                this.setNoGravity(true);
                Player chaser = examinee != null ? level.getPlayerByUUID(examinee) : level.getNearestPlayer(this, 24.0D);
                if (chaser != null) {
                    Messages.actionBar(chaser, Component.translatable("message.frieren.stille_recovered").withStyle(ChatFormatting.YELLOW));
                }
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
        if (source.getEntity() instanceof Player player) {
            if (!isExhausted()) {
                if (++hits >= HITS_TO_EXHAUST) {
                    exhaustedTicks = EXHAUST_TICKS;
                    Messages.actionBar(player, Component.translatable("message.frieren.stille_exhausted")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PARROT_HURT, SoundSource.NEUTRAL, 1.0F, 1.6F);
                    level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3D, this.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
                } else {
                    Messages.actionBar(player, Component.translatable("message.frieren.stille_tiring", hitsToExhaust()));
                }
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
