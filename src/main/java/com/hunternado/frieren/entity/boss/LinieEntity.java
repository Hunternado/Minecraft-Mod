package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Linie, one of Aura's three generals, who copies the fighting style of warriors she has watched. Fights up close:
 * dashes in, then imitates Eisen's overhead slam (a short telegraph, a heavy hit) or a spinning slash.
 */
public class LinieEntity extends DemonBoss {
    private static final int COLOR = 0xD04A8A;

    public LinieEntity(EntityType<? extends LinieEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PINK);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 180.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.32D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
    }

    @Override protected float baseHealth() { return 180.0F; }
    @Override protected float baseDamage() { return 7.0F; }
    @Override protected String bossId() { return "linie"; }
    @Override public int bonusGrowthReward() { return 15; }
    @Override public float trueMana() { return 900.0F; }
    @Override public int concealLevel() { return 2; }
    @Override public byte senseKind() { return 2; }

    @Override
    protected void move(ServerLevel level, LivingEntity target) {
        // MeleeAttackGoal does the chasing.
    }

    @Override
    protected int attack(ServerLevel level, LivingEntity target, int phase) {
        if (this.getRandom().nextBoolean()) {
            // Dash in and imitate Eisen's overhead slam.
            Vec3 dash = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(1.3D);
            this.setDeltaMovement(dash.x, 0.25D, dash.z);
            this.needsSync = true;
            telegraphBlast(level, target.position(), 2.2F, 14, 9.0F, MagicCircleEntity.STYLE_DEMON, COLOR);
            SpellFx.streaks(level, this.position().add(0.0D, 1.0D, 0.0D), COLOR, 1.0F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
        } else {
            // Spinning slash around herself.
            Vec3 center = this.position();
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3.0D;
                telegraphBlast(level, center.add(Math.cos(angle) * 2.5D, 0.0D, Math.sin(angle) * 2.5D), 1.6F, 12, 7.0F,
                    MagicCircleEntity.STYLE_DEMON, COLOR);
            }
            setCasting(12);
        }
        return phase >= 2 ? 45 : 60;
    }
}
