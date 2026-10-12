package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Demon King, whom Himmel's party defeated. Three phases: dark Zoltraak and crimson rain with summoned
 * soldiers; then the Black Sun (expanding rings of demonic blasts) and fanned beams; finally, below a quarter of his
 * health, he blinks around the battlefield and fires in volleys.
 */
public class DemonKingEntity extends DemonBoss {
    private static final int COLOR = 0xB01030;
    private int chargeTicks;
    private int volleys;
    private Vec3 aim = Vec3.ZERO;

    public DemonKingEntity(EntityType<? extends DemonKingEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 600.0D)
            .add(Attributes.ATTACK_DAMAGE, 14.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.24D)
            .add(Attributes.ARMOR, 10.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override protected float baseHealth() { return 600.0F; }
    @Override protected float baseDamage() { return 14.0F; }
    @Override protected float[] phaseThresholds() { return new float[] {0.6F, 0.25F}; }
    @Override protected String bossId() { return "demon_king"; }
    @Override public int bonusGrowthReward() { return 100; }
    @Override public float trueMana() { return 20000.0F; }
    @Override public int concealLevel() { return 0; }
    @Override public byte senseKind() { return 2; }

    @Override
    protected void move(ServerLevel level, LivingEntity target) {
        keepDistance(target, 8.0D, 15.0D);
    }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (chargeTicks <= 0) {
            return;
        }
        faceTowards(aim);
        if (--chargeTicks > 0) {
            return;
        }
        Vec3 direction = aim.subtract(this.getEyePosition()).normalize();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        fireBeam(level, direction, 40.0D, damage, COLOR, 0.7F, ModDamageTypes.DEMON_MAGIC);
        if (phase() >= 2) {
            fireBeam(level, direction.yRot(0.25F), 40.0D, damage * 0.7F, COLOR, 0.5F, ModDamageTypes.DEMON_MAGIC);
            fireBeam(level, direction.yRot(-0.25F), 40.0D, damage * 0.7F, COLOR, 0.5F, ModDamageTypes.DEMON_MAGIC);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.HOSTILE, 1.6F, 0.6F);
        if (--volleys > 0) {
            beginCharge(level, target, 14);
        }
    }

    private void beginCharge(ServerLevel level, LivingEntity target, int ticks) {
        aim = target.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        chargeTicks = ticks;
        setCasting(ticks + 2);
        faceTowards(aim);
        MagicCircleEntity.inFront(level, this, MagicCircleEntity.STYLE_DEMON, COLOR, 1.8F, ticks + 4, 1.4F);
    }

    @Override
    protected int attack(ServerLevel level, LivingEntity target, int phase) {
        if (chargeTicks > 0) {
            return 10;
        }
        if (phase >= 3 && this.getRandom().nextFloat() < 0.4F) {
            blink(level, target);
        }
        switch (this.getRandom().nextInt(phase >= 2 ? 4 : 3)) {
            case 0 -> {
                volleys = phase >= 3 ? 3 : 1;
                beginCharge(level, target, phase >= 3 ? 18 : 28);
            }
            case 1 -> {
                // Crimson rain: blasts on and around the target.
                Vec3 center = target.position();
                telegraphBlast(level, center, 2.2F, 30, 10.0F, MagicCircleEntity.STYLE_DEMON, COLOR);
                for (int i = 0; i < 4 + phase; i++) {
                    double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
                    double r = 2.5D + this.getRandom().nextDouble() * 4.0D;
                    telegraphBlast(level, center.add(Math.cos(angle) * r, 0.0D, Math.sin(angle) * r), 2.0F, 30 + i * 3, 9.0F,
                        MagicCircleEntity.STYLE_DEMON, COLOR);
                }
                setCasting(20);
            }
            case 2 -> {
                summonMinion(level, ModEntities.DEMON_SOLDIER.get(), target, 4);
                summonMinion(level, phase >= 2 ? ModEntities.DEMON_MAGE.get() : ModEntities.DEMON_SOLDIER.get(), target, 4);
                setCasting(16);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.4F, 0.6F);
            }
            default -> blackSun(level);
        }
        return phase >= 3 ? 40 : phase == 2 ? 55 : 70;
    }

    /** The Black Sun: two expanding rings of demonic blasts around the Demon King. */
    private void blackSun(ServerLevel level) {
        Vec3 center = this.position();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D;
            telegraphBlast(level, center.add(Math.cos(angle) * 4.0D, 0.0D, Math.sin(angle) * 4.0D), 2.0F, 22, 10.0F,
                MagicCircleEntity.STYLE_DEMON, COLOR);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6.0D + 0.26D;
            telegraphBlast(level, center.add(Math.cos(angle) * 8.0D, 0.0D, Math.sin(angle) * 8.0D), 2.2F, 38, 10.0F,
                MagicCircleEntity.STYLE_DEMON, COLOR);
        }
        SpellFx.wisps(level, center.add(0.0D, 2.5D, 0.0D), COLOR, 3.0F);
        setCasting(30);
    }

    /** Phase 3: vanish and reappear beside the target. */
    private void blink(ServerLevel level, LivingEntity target) {
        Vec3 from = this.position();
        double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
        Vec3 to = target.position().add(Math.cos(angle) * 6.0D, 0.0D, Math.sin(angle) * 6.0D);
        if (this.randomTeleport(to.x, to.y, to.z, false)) {
            SpellFx.wisps(level, from.add(0.0D, 1.0D, 0.0D), COLOR, 1.5F);
            SpellFx.burst(level, this.position().add(0.0D, 1.0D, 0.0D), COLOR, 1.5F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TELEPORT.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }
}
