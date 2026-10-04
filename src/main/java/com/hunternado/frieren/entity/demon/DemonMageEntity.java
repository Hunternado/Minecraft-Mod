package com.hunternado.frieren.entity.demon;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.ai.HuntPlayersGoal;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.RangedSpellGoal;
import com.hunternado.frieren.entity.ai.WanderGoal;
import com.hunternado.frieren.entity.magic.ManaBoltEntity;
import com.hunternado.frieren.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Tier 2 demon: keeps its distance, fires demon bolts, hides behind a regenerating mana barrier and
 * conceals its true mana from detection.
 */
public class DemonMageEntity extends DemonEntity implements RangedSpellGoal.Caster {
    public DemonMageEntity(EntityType<? extends DemonMageEntity> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 26.0D)
            .add(Attributes.ATTACK_DAMAGE, 4.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.27D)
            .add(Attributes.ARMOR, 2.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedSpellGoal(this, this, 1.0D, 6.0D, 16.0D, castInterval()));
        this.goalSelector.addGoal(5, new WanderGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new IdleLookGoal(this, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayersGoal(this));
    }

    protected int castInterval() {
        return 40;
    }

    protected float boltDamage() {
        return 6.0F;
    }

    protected int boltColor() {
        return 0xC03A5A;
    }

    @Override
    protected float maxBarrier() {
        return 20.0F;
    }

    @Override
    public boolean canCastNow() {
        return !isPleading();
    }

    @Override
    public void castAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ManaBoltEntity.shoot(level, this, target.getEyePosition(), boltDamage(), 1.1F, boltColor(), ModDamageTypes.DEMON_MAGIC, 1.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.DEMON_BOLT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public float trueMana() {
        return 140.0F;
    }

    @Override
    public int concealLevel() {
        return 4;
    }
}
