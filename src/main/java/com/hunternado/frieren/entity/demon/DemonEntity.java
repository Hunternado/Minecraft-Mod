package com.hunternado.frieren.entity.demon;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.combat.SpellDamageSource;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base of every demon. Demons are mechanically distinct from ordinary monsters:
 * <ul>
 *   <li>a mana signature they can conceal ({@link ManaSignature}),</li>
 *   <li>an optional regenerating mana barrier (absorbs damage; some spells cut through it),</li>
 *   <li><b>deception</b>: when badly hurt they may plead in human words; a player who stops attacking is
 *   punished with a treacherous strike — demons use language to deceive, not to communicate.</li>
 * </ul>
 */
public abstract class DemonEntity extends Monster implements ManaSignature {
    private static final EntityDataAccessor<Float> DATA_BARRIER = SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.FLOAT);
    private static final int PLEA_LINES = 4;

    private int pleadTicks;
    private boolean hasPleaded;
    private boolean pleaInterrupted;
    private int lastHurtTick = -1000;

    protected DemonEntity(EntityType<? extends DemonEntity> type, Level level) {
        super(type, level);
    }

    /** Demons placed by structures (camps, fortresses) stay put instead of despawning like natural spawns. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData groupData) {
        if (reason == EntitySpawnReason.STRUCTURE) {
            this.setPersistenceRequired();
        }
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    // ---- Mana barrier ------------------------------------------------------------------------

    /** Maximum barrier strength; 0 disables the barrier. */
    protected float maxBarrier() {
        return 0.0F;
    }

    public float barrier() {
        return this.entityData.get(DATA_BARRIER);
    }

    protected void setBarrier(float value) {
        this.entityData.set(DATA_BARRIER, Math.max(0.0F, Math.min(maxBarrier(), value)));
    }

    public boolean isPleading() {
        return pleadTicks > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BARRIER, 0.0F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (pleadTicks > 0 && source.getEntity() instanceof Player) {
            // The player saw through the lie.
            pleadTicks = 0;
            pleaInterrupted = true;
        }
        float remaining = amount;
        float barrier = barrier();
        if (barrier > 0.0F && remaining > 0.0F) {
            float cost = source instanceof SpellDamageSource spellSource ? spellSource.barrierCost() : 1.0F;
            float absorbed = Math.min(remaining, barrier / Math.max(0.05F, cost));
            setBarrier(barrier - absorbed * cost);
            remaining -= absorbed;
            SpellFx.hexagon(level, this.position().add(0, this.getBbHeight() * 0.55D, 0), 0xB03060, 1.0F);
            if (barrier() <= 0.0F) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BARRIER_BREAK.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
            }
        }
        lastHurtTick = this.tickCount;
        boolean hurt = remaining > 0.0F ? super.hurtServer(level, source, remaining) : remaining == 0.0F && amount > 0.0F;
        if (hurt && !hasPleaded && canDeceive() && FrierenConfig.demonDeception() && this.isAlive()
            && this.getHealth() < this.getMaxHealth() * 0.3F && this.getRandom().nextFloat() < 0.6F) {
            beginPlea(level);
        }
        return hurt;
    }

    /** Bosses and thralls don't plead. */
    protected boolean canDeceive() {
        return true;
    }

    private void beginPlea(ServerLevel level) {
        hasPleaded = true;
        pleaInterrupted = false;
        pleadTicks = 60;
        this.setTarget(null);
        this.getNavigation().stop();
        Component line = Component.translatable(FrierenIds.key("demon", "plea." + this.getRandom().nextInt(PLEA_LINES)))
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        Messages.nearby(level, this.position(), 16.0D, Component.literal("<").append(this.getDisplayName()).append("> ").append(line));
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.DEMON_SPEAK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void deceptionStrike(ServerLevel level) {
        Player victim = level.getNearestPlayer(this, 8.0D);
        if (victim == null || victim.isCreative() || victim.isSpectator()) {
            return;
        }
        Messages.nearby(level, this.position(), 16.0D, Component.translatable("message.frieren.deceived", this.getDisplayName())
            .withStyle(ChatFormatting.DARK_RED));
        this.setTarget(victim);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F;
        victim.hurtServer(level, this.damageSources().mobAttack(this), damage);
        SpellFx.burst(level, victim.position().add(0, 1.0D, 0), 0x8B0000, 0.8F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (pleadTicks > 0) {
            this.setTarget(null);
            this.getNavigation().stop();
            if (--pleadTicks == 0 && !pleaInterrupted) {
                deceptionStrike(level);
            }
        }
        if (maxBarrier() > 0.0F && this.tickCount - lastHurtTick > 100 && barrier() < maxBarrier() && this.tickCount % 5 == 0) {
            setBarrier(barrier() + maxBarrier() * 0.05F);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            // Demons leave no corpse; they dissolve into mana.
            SpellFx.burst(level, this.position().add(0, this.getBbHeight() * 0.5D, 0), 0x6A2C70, 1.4F);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasPleaded", hasPleaded);
        output.putFloat("Barrier", barrier());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        hasPleaded = input.getBooleanOr("HasPleaded", false);
        setBarrier(input.getFloatOr("Barrier", maxBarrier()));
    }
}
