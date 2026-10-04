package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.entity.demon.DemonMageEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Qual the Corruption, sealed for eighty years. Inventor of Zoltraak, the original killing magic: his beam
 * pierces armour, but modern defensive magic stops it cheaply (the very lesson humanity learned from him).
 * Charges a telegraphed beam, alternates with demon bolts and blinks away when pressed.
 */
public class QualEntity extends DemonMageEntity implements FrierenBoss {
    private static final float BASE_HEALTH = 160.0F;
    private static final int CHARGE_TICKS = 15;

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
        Component.translatable("entity.frieren.qual"), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
    private boolean scaled;
    private int castCount;
    private int chargeTicks;
    private int blinkCooldown;
    private Vec3 chargeTarget = Vec3.ZERO;

    public QualEntity(EntityType<? extends QualEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, BASE_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, 6.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.27D)
            .add(Attributes.ARMOR, 6.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
            .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public int bonusGrowthReward() {
        return 20;
    }

    @Override
    protected int castInterval() {
        return 45;
    }

    @Override
    protected float boltDamage() {
        return (float) (6.0D * FrierenConfig.bossDamageMultiplier());
    }

    @Override
    protected int boltColor() {
        return 0x9B2D6F;
    }

    @Override
    protected float maxBarrier() {
        return 40.0F;
    }

    @Override
    public float trueMana() {
        return 420.0F;
    }

    @Override
    public int concealLevel() {
        return 3;
    }

    @Override
    public boolean canCastNow() {
        return super.canCastNow() && chargeTicks <= 0;
    }

    @Override
    public void castAt(LivingEntity target) {
        if (++castCount % 2 == 0 && this.level() instanceof ServerLevel level) {
            // Telegraph the original Zoltraak, fire it a moment later.
            chargeTicks = CHARGE_TICKS;
            chargeTarget = target.getEyePosition();
            SpellFx.burst(level, this.getEyePosition(), 0x9B2D6F, 0.6F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SPELL_CAST.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
        } else {
            super.castAt(target);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!scaled) {
            scaled = true;
            AuraEntity.applyDifficultyScaling(this, BASE_HEALTH, 6.0F);
        }
        BossBarHelper.update(bossBar, level, this);
        if (blinkCooldown > 0) {
            blinkCooldown--;
        }
        if (chargeTicks > 0 && --chargeTicks == 0) {
            fireOriginalZoltraak(level);
        }
    }

    private void fireOriginalZoltraak(ServerLevel level) {
        Vec3 from = this.getEyePosition();
        LivingEntity target = this.getTarget();
        Vec3 aim = target != null ? target.getEyePosition() : chargeTarget;
        Vec3 to = from.add(aim.subtract(from).normalize().scale(40.0D));
        List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(level, this, from, to, 0.3D,
            entity -> SpellDamage.canHarm(level, this, entity));
        float damage = (float) (12.0D * FrierenConfig.bossDamageMultiplier());
        for (SpellTargeting.EntityHit hit : hits) {
            // Pierces everything; barriers stop it at a third of the usual mana cost.
            SpellDamage.hurt(level, this, this, hit.entity(), damage, ModDamageTypes.DEMON_MAGIC, 0.33F, false, null);
            damage *= 0.85F;
        }
        SpellFx.beam(level, from, to, 0x9B2D6F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && blinkCooldown <= 0 && this.isAlive() && this.getRandom().nextFloat() < 0.35F) {
            blinkCooldown = 100;
            double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
            SpellFx.burst(level, this.position().add(0, 1.0D, 0), 0x9B2D6F, 0.8F);
            this.teleportTo(this.getX() + Math.cos(angle) * 8.0D, this.getY(), this.getZ() + Math.sin(angle) * 8.0D);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TELEPORT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossBar.removeAllPlayers();
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
