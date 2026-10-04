package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.exam.ExamManager;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spiegel, the mirror of the Ruined King's Tomb (Second-Class and First-Class exam boss). It hides behind
 * mirror replicas of the examinees around it and is only vulnerable while every replica is broken.
 */
public class SpiegelEntity extends Monster implements FrierenBoss, ManaSignature {
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED = SynchedEntityData.defineId(SpiegelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float BASE_HEALTH = 200.0F;
    private static final int EXPOSED_TICKS = 200;

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
        Component.translatable("entity.frieren.spiegel"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
    private final List<UUID> replicas = new ArrayList<>();
    private boolean scaled;
    private boolean waveActive;
    private int exposedTicks;
    private int waveDelay = 40;

    public SpiegelEntity(EntityType<? extends SpiegelEntity> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, BASE_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, 5.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.18D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new IdleLookGoal(this, 16.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EXPOSED, false);
    }

    public boolean isExposed() {
        return this.entityData.get(DATA_EXPOSED);
    }

    @Override
    public int bonusGrowthReward() {
        return 15;
    }

    @Override
    public float trueMana() {
        return 250.0F;
    }

    @Override
    public int concealLevel() {
        return isExposed() ? 0 : 6;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!scaled) {
            scaled = true;
            AuraEntity.applyDifficultyScaling(this, BASE_HEALTH, 5.0F);
        }
        BossBarHelper.update(bossBar, level, this);
        if (this.tickCount % 10 != 0) {
            return;
        }
        replicas.removeIf(id -> {
            Entity replica = level.getEntity(id);
            return replica == null || !replica.isAlive();
        });
        if (isExposed()) {
            exposedTicks -= 10;
            if (exposedTicks <= 0) {
                setExposed(false);
                waveDelay = 20;
            }
        } else if (replicas.isEmpty()) {
            if (waveDelay > 0) {
                waveDelay -= 10;
            } else if (bossBar.getPlayers().isEmpty()) {
                waveDelay = 40;
            } else if (waveActive) {
                // Every replica of the last wave is broken: the mirror cracks.
                waveActive = false;
                setExposed(true);
                exposedTicks = EXPOSED_TICKS;
                Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                    Component.translatable("boss.frieren.spiegel.exposed").withStyle(ChatFormatting.AQUA));
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BARRIER_BREAK.get(), SoundSource.HOSTILE, 1.5F, 0.6F);
            } else {
                spawnWave(level);
            }
        }
    }

    private void setExposed(boolean exposed) {
        this.entityData.set(DATA_EXPOSED, exposed);
        this.setInvisible(!exposed && !replicas.isEmpty());
    }

    private void spawnWave(ServerLevel level) {
        int extra = this.getHealth() < this.getMaxHealth() * 0.5F ? 1 : 0;
        int spawned = 0;
        for (ServerPlayer player : List.copyOf(bossBar.getPlayers())) {
            if (spawned >= 4) {
                break;
            }
            spawned += spawnReplica(level, player) ? 1 : 0;
        }
        for (int i = 0; i < extra && !bossBar.getPlayers().isEmpty(); i++) {
            spawnReplica(level, bossBar.getPlayers().iterator().next());
        }
        if (!replicas.isEmpty()) {
            waveActive = true;
            this.setInvisible(true);
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.spiegel.reflect").withStyle(ChatFormatting.GRAY));
        }
    }

    private boolean spawnReplica(ServerLevel level, ServerPlayer source) {
        MirrorReplicaEntity replica = ModEntities.MIRROR_REPLICA.get().create(level, EntitySpawnReason.REINFORCEMENT);
        if (replica == null) {
            return false;
        }
        double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
        replica.snapTo(this.getX() + Math.cos(angle) * 4.0D, this.getY(), this.getZ() + Math.sin(angle) * 4.0D, this.getYRot(), 0.0F);
        replica.copyFrom(source, this);
        level.addFreshEntity(replica);
        replicas.add(replica.getUUID());
        SpellFx.burst(level, replica.position().add(0, 1.0D, 0), 0xDDEEFF, 0.8F);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isExposed()) {
            if (source.getEntity() instanceof Player player && this.getRandom().nextInt(4) == 0) {
                Messages.actionBar(player, Component.translatable("boss.frieren.spiegel.immune"));
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossBar.removeAllPlayers();
        if (this.level() instanceof ServerLevel level) {
            for (UUID id : replicas) {
                Entity replica = level.getEntity(id);
                if (replica != null) {
                    replica.discard();
                }
            }
            ExamManager.onSpiegelDefeated(level, this.position());
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Scaled", scaled);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        scaled = input.getBooleanOr("Scaled", false);
    }
}
