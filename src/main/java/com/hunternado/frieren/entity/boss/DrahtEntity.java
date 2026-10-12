package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
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
 * Draht, one of Aura's three generals, who fights with invisible wires: he snares his target in place, then cuts with
 * a fan of thin threads. Once wounded his snare catches everyone around the target.
 */
public class DrahtEntity extends DemonBoss {
    private static final int COLOR = 0xE8E8F0;

    public DrahtEntity(EntityType<? extends DrahtEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 160.0D)
            .add(Attributes.ATTACK_DAMAGE, 6.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 3.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override protected float baseHealth() { return 160.0F; }
    @Override protected float baseDamage() { return 6.0F; }
    @Override protected String bossId() { return "draht"; }
    @Override public int bonusGrowthReward() { return 15; }
    @Override public float trueMana() { return 800.0F; }
    @Override public int concealLevel() { return 3; }
    @Override public byte senseKind() { return 2; }

    @Override
    protected void move(ServerLevel level, LivingEntity target) {
        keepDistance(target, 6.0D, 12.0D);
    }

    @Override
    protected int attack(ServerLevel level, LivingEntity target, int phase) {
        Vec3 hand = this.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        if (this.getRandom().nextBoolean()) {
            for (LivingEntity victim : SpellTargeting.livingInRadius(level, this, target.position(), phase >= 2 ? 8.0D : 0.5D,
                candidate -> candidate == target || isFoe(level, candidate))) {
                victim.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.BOUND), 30, 0));
                SpellDamage.hurt(level, this, this, victim, 3.0F, ModDamageTypes.DEMON_MAGIC, 1.0F, false, null);
                SpellFx.beam(level, hand, victim.position().add(0.0D, victim.getBbHeight() * 0.6D, 0.0D), COLOR, 0.05F, 30,
                    SpellBeamEntity.STYLE_LIGHT);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CURSE.get(), SoundSource.HOSTILE, 1.0F, 1.8F);
        } else {
            Vec3 direction = target.getEyePosition().subtract(this.getEyePosition()).normalize();
            faceTowards(target.getEyePosition());
            for (int i = -1; i <= 1; i++) {
                fireBeam(level, direction.yRot(i * 0.18F), 24.0D, 6.0F, COLOR, 0.08F, ModDamageTypes.DEMON_MAGIC);
            }
            setCasting(10);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 1.0F, 1.6F);
        }
        return phase >= 2 ? 40 : 55;
    }
}
