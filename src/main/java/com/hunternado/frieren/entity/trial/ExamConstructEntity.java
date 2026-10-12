package com.hunternado.frieren.entity.trial;

import com.hunternado.frieren.combat.SpellDamageSource;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Fifth-Class practical exam target: a floating mana crystal that drifts on a slow orbit around its anchor and
 * only breaks to spells. No AI, no gravity, no collision with the world.
 */
public class ExamConstructEntity extends Mob {
    private Vec3 anchor = Vec3.ZERO;
    private double orbitRadius;
    private double angle;
    private double speed;
    private boolean anchored;

    public ExamConstructEntity(EntityType<? extends ExamConstructEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Places the construct on an orbit around {@code anchor}; a radius of 0 keeps it hovering in place. */
    public void setOrbit(Vec3 anchor, double radius, double startAngle, double speed) {
        this.anchor = anchor;
        this.orbitRadius = radius;
        this.angle = startAngle;
        this.speed = speed;
        this.anchored = true;
        Vec3 p = orbitPosition();
        this.snapTo(p.x, p.y, p.z, 0.0F, 0.0F);
    }

    private Vec3 orbitPosition() {
        double bob = Math.sin(this.tickCount * 0.08D + angle * 3.0D) * 0.25D;
        return anchor.add(Math.cos(angle) * orbitRadius, bob, Math.sin(angle) * orbitRadius);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !anchored) {
            return;
        }
        angle += speed;
        Vec3 p = orbitPosition();
        this.setPos(p.x, p.y, p.z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!(source instanceof SpellDamageSource) && !source.isCreativePlayer()) {
            if (source.getEntity() instanceof Player player) {
                Messages.actionBar(player, Component.translatable("message.frieren.construct_magic_only"));
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            SpellFx.burst(level, this.position().add(0.0D, 0.5D, 0.0D), 0x9FD8FF, 1.0F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BARRIER_BREAK.get(), SoundSource.HOSTILE, 0.8F, 1.6F);
            this.discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("AnchorX", anchor.x);
        output.putDouble("AnchorY", anchor.y);
        output.putDouble("AnchorZ", anchor.z);
        output.putDouble("Orbit", orbitRadius);
        output.putDouble("Angle", angle);
        output.putDouble("Speed", speed);
        output.putBoolean("Anchored", anchored);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        anchor = new Vec3(input.getDoubleOr("AnchorX", 0.0D), input.getDoubleOr("AnchorY", 0.0D), input.getDoubleOr("AnchorZ", 0.0D));
        orbitRadius = input.getDoubleOr("Orbit", 0.0D);
        angle = input.getDoubleOr("Angle", 0.0D);
        speed = input.getDoubleOr("Speed", 0.0D);
        anchored = input.getBooleanOr("Anchored", false);
    }
}
