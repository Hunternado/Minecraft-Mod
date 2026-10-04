package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.ai.FollowLeaderGoal;
import com.hunternado.frieren.entity.ai.HuntHostilesGoal;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.RangedSpellGoal;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Frieren's companions. Fern snipes hostile mobs with rapid Zoltraak; Stark fights in melee (bravely,
 * despite the shaking knees). Neither ever attacks players.
 */
public class CompanionEntity extends FrierenNpcBase implements RangedSpellGoal.Caster {
    public enum Role { FERN, STARK }

    private @Nullable UUID leaderId;
    private @Nullable LivingEntity cachedLeader;
    private boolean statsApplied;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
    }

    public static CompanionEntity fern(EntityType<CompanionEntity> type, Level level) {
        return new CompanionEntity(type, level);
    }

    public static CompanionEntity stark(EntityType<CompanionEntity> type, Level level) {
        return new CompanionEntity(type, level);
    }

    /**
     * Derived from the entity type rather than a constructor argument: {@code registerGoals()} runs inside
     * the super constructor, before subclass fields are assigned.
     */
    public Role role() {
        return this.getType() == ModEntities.STARK.get() ? Role.STARK : Role.FERN;
    }

    public void setLeader(LivingEntity leader) {
        this.leaderId = leader.getUUID();
        this.cachedLeader = leader;
    }

    private @Nullable LivingEntity leader() {
        if ((cachedLeader == null || !cachedLeader.isAlive()) && leaderId != null && this.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(leaderId);
            cachedLeader = entity instanceof LivingEntity living ? living : null;
        }
        return cachedLeader;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (role() == Role.FERN) {
            this.goalSelector.addGoal(2, new RangedSpellGoal(this, this, 1.0D, 4.0D, 18.0D, 20));
        } else {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        }
        this.goalSelector.addGoal(4, new FollowLeaderGoal(this, this::leader, 1.0D, 6.0D, 3.0D));
        this.goalSelector.addGoal(7, new IdleLookGoal(this, 8.0F));
        this.targetSelector.addGoal(1, new HuntHostilesGoal(this, 16.0D));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!statsApplied && !this.level().isClientSide()) {
            statsApplied = true;
            AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (role() == Role.STARK && health != null && damage != null) {
                health.setBaseValue(80.0D);
                damage.setBaseValue(12.0D);
                this.setHealth(this.getMaxHealth());
            }
        }
    }

    @Override
    public void castAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level) || role() != Role.FERN) {
            return;
        }
        SpellDamage.hurt(level, this, this, target, 7.0F, ModDamageTypes.ZOLTRAAK, 1.0F, false, null);
        SpellFx.beam(level, this.getEyePosition(), target.getEyePosition(), 0xB8E0FF);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.NEUTRAL, 0.8F, 1.4F);
    }

    @Override
    protected String npcId() {
        return role() == Role.FERN ? "fern" : "stark";
    }

    @Override
    public float trueMana() {
        return role() == Role.FERN ? 1200.0F : 20.0F;
    }

    @Override
    public int concealLevel() {
        return role() == Role.FERN ? 7 : 0;
    }

    @Override
    public void onInteract(ServerPlayer player) {
        sayRandomLine(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (leaderId != null) {
            output.putString("Leader", leaderId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String id = input.getStringOr("Leader", "");
        try {
            leaderId = id.isEmpty() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            leaderId = null;
        }
    }
}
