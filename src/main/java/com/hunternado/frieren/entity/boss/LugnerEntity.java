package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.magic.ManaBoltEntity;
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
 * Lügner, one of Aura's three generals, a blood mage and a liar to the end. Throws fans of blood blades that heal
 * him, opens pools of blood under his target, and once wounded closes his own wounds over time.
 */
public class LugnerEntity extends DemonBoss {
    private static final int COLOR = 0xB0102A;

    public LugnerEntity(EntityType<? extends LugnerEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 190.0D)
            .add(Attributes.ATTACK_DAMAGE, 6.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.27D)
            .add(Attributes.ARMOR, 3.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override protected float baseHealth() { return 190.0F; }
    @Override protected float baseDamage() { return 6.0F; }
    @Override protected String bossId() { return "lugner"; }
    @Override public int bonusGrowthReward() { return 15; }
    @Override public float trueMana() { return 850.0F; }
    @Override public int concealLevel() { return 4; }
    @Override public byte senseKind() { return 2; }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (phase() >= 2 && this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.5F);
            SpellFx.wisps(level, this.position().add(0.0D, 1.0D, 0.0D), COLOR, 0.5F);
        }
    }

    @Override
    protected int attack(ServerLevel level, LivingEntity target, int phase) {
        if (this.getRandom().nextBoolean()) {
            Vec3 eye = this.getEyePosition();
            Vec3 direction = target.getEyePosition().subtract(eye).normalize();
            faceTowards(target.getEyePosition());
            for (int i = 0; i < 5; i++) {
                Vec3 bladeDirection = direction.yRot((float) Math.toRadians((i - 2) * 9.0D));
                ManaBoltEntity bolt = ManaBoltEntity.shoot(level, this, eye.add(bladeDirection.scale(10.0D)), 5.0F, 1.5F, COLOR,
                    ModDamageTypes.DEMON_MAGIC, 1.5F);
                if (bolt != null) {
                    bolt.withLifesteal(0.5F);
                }
            }
            setCasting(10);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.DEMON_BOLT.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
        } else {
            telegraphBlast(level, target.position(), 2.6F, 24, 9.0F, MagicCircleEntity.STYLE_DEMON, COLOR);
            setCasting(16);
        }
        return phase >= 2 ? 45 : 60;
    }
}
