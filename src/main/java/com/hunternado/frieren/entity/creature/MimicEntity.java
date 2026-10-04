package com.hunternado.frieren.entity.creature;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.ai.HuntPlayersGoal;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.quest.QuestStats;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A treasure chest that is not a treasure chest. While disguised it has no AI at all (so it sits perfectly
 * still); opening it gets you eaten. Mimic Detection reveals it, with the canonical 99% accuracy.
 */
public class MimicEntity extends Monster implements ManaSignature {
    private static final EntityDataAccessor<Boolean> DATA_REVEALED = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.ARMOR, 6.0D)
            .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayersGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEALED, false);
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
    }

    public void reveal(Player revealer) {
        if (!isRevealed()) {
            this.entityData.set(DATA_REVEALED, true);
            this.setNoAi(false);
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
        if (revealer != null && !revealer.isCreative()) {
            this.setTarget(revealer);
        }
    }

    /** Somebody tried to open the "chest". */
    public void chomp(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Messages.chat(player, Component.translatable("message.frieren.mimic_bite").withStyle(ChatFormatting.DARK_PURPLE));
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MIMIC_CHOMP.get(), SoundSource.HOSTILE, 1.2F, 0.8F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
        player.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
        MagicData data = MagicData.get(player);
        if (data != null) {
            data.incrementStat(QuestStats.MIMIC_BITES);
            // Being eaten is, technically, research.
            data.incrementStat(QuestStats.MIMIC_RESEARCH);
            QuestManager.onStat(player, data, QuestStats.MIMIC_RESEARCH);
        }
        reveal(player);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isRevealed() && source.getEntity() instanceof Player player) {
            reveal(player);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public float trueMana() {
        return 30.0F;
    }

    @Override
    public int concealLevel() {
        return isRevealed() ? 0 : 6;
    }

    @Override
    public byte senseKind() {
        return 3;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Revealed", isRevealed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean revealed = input.getBooleanOr("Revealed", false);
        this.entityData.set(DATA_REVEALED, revealed);
        this.setNoAi(!revealed);
    }
}
