package com.hunternado.frieren.entity.demon;

import com.hunternado.frieren.entity.ai.HuntPlayersGoal;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.WanderGoal;
import com.hunternado.frieren.mana.ManaSignature;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * One of Aura's headless soldiers: an armoured corpse moved by her Scales of Obedience. Slow, tough and
 * relentless; its faint mana is Aura's own thread.
 */
public class AuraThrallEntity extends Monster implements ManaSignature {
    public AuraThrallEntity(EntityType<? extends AuraThrallEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.22D)
            .add(Attributes.ARMOR, 10.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
            .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WanderGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new IdleLookGoal(this, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayersGoal(this));
    }

    /** Thralls standing guard in Aura's fortress never despawn. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        if (reason == EntitySpawnReason.STRUCTURE) {
            this.setPersistenceRequired();
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public float trueMana() {
        return 15.0F;
    }

    @Override
    public int concealLevel() {
        return 0;
    }
}
