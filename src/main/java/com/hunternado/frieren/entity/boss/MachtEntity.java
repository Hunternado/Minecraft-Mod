package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Macht of the Golden Land, one of the Seven Sages of Destruction, who turned a whole city to gold. His Diagolze ray
 * gilds whoever it touches for a moment (rooted and harmless), golden spikes march across the ground, and once
 * wounded he calls down the Golden Land: gold erupting everywhere at once.
 */
public class MachtEntity extends DemonBoss {
    private static final int GOLD = 0xFFC83A;
    private int chargeTicks;
    private Vec3 aim = Vec3.ZERO;

    public MachtEntity(EntityType<? extends MachtEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 420.0D)
            .add(Attributes.ATTACK_DAMAGE, 10.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.23D)
            .add(Attributes.ARMOR, 8.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override protected float baseHealth() { return 420.0F; }
    @Override protected float baseDamage() { return 10.0F; }
    @Override protected String bossId() { return "macht"; }
    @Override public int bonusGrowthReward() { return 60; }
    @Override public float trueMana() { return 8000.0F; }
    @Override public int concealLevel() { return 3; }
    @Override public byte senseKind() { return 2; }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (chargeTicks <= 0) {
            return;
        }
        faceTowards(aim);
        if (--chargeTicks > 0) {
            return;
        }
        LivingEntity hit = fireBeam(level, aim.subtract(this.getEyePosition()), 32.0D, 6.0F, GOLD, 0.5F, ModDamageTypes.CURSE);
        if (hit != null) {
            hit.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.GILDED), 40, 0));
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CURSE.get(), SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    @Override
    protected int attack(ServerLevel level, LivingEntity target, int phase) {
        if (chargeTicks > 0) {
            return 10;
        }
        switch (this.getRandom().nextInt(phase >= 2 ? 3 : 2)) {
            case 0 -> {
                aim = target.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
                chargeTicks = 22;
                setCasting(24);
                faceTowards(aim);
                MagicCircleEntity.inFront(level, this, MagicCircleEntity.STYLE_HOLY, GOLD, 1.4F, 26, 1.2F);
            }
            case 1 -> {
                // Golden spikes marching toward the target.
                Vec3 from = this.position();
                Vec3 step = target.position().subtract(from).multiply(1.0D, 0.0D, 1.0D).normalize().scale(2.0D);
                for (int i = 1; i <= 7; i++) {
                    telegraphBlast(level, from.add(step.scale(i)), 1.5F, 10 + i * 4, 8.0F, MagicCircleEntity.STYLE_HOLY, GOLD);
                }
                setCasting(14);
            }
            default -> {
                // The Golden Land.
                Vec3 center = target.position();
                for (int i = 0; i < 10; i++) {
                    double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
                    double r = this.getRandom().nextDouble() * 9.0D;
                    telegraphBlast(level, center.add(Math.cos(angle) * r, 0.0D, Math.sin(angle) * r), 2.5F, 30, 9.0F,
                        MagicCircleEntity.STYLE_HOLY, GOLD);
                }
                setCasting(30);
            }
        }
        return phase >= 2 ? 50 : 65;
    }
}
