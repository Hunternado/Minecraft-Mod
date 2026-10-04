package com.hunternado.frieren.entity.demon;

import com.hunternado.frieren.entity.ai.HuntPlayersGoal;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.WanderGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Tier 1 demon: horned melee fighter that lies when cornered. */
public class DemonSoldierEntity extends DemonEntity {
    public DemonSoldierEntity(EntityType<? extends DemonSoldierEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 32.0D)
            .add(Attributes.ATTACK_DAMAGE, 6.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new WanderGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new IdleLookGoal(this, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayersGoal(this));
    }

    @Override
    public float trueMana() {
        return 80.0F;
    }

    @Override
    public int concealLevel() {
        return 1;
    }
}
