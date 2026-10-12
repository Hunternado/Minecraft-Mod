package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.client.render.character.CharacterPoses;
import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.ai.HuntPlayersGoal;
import com.hunternado.frieren.entity.fx.BarrierShellEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * The mirror replica of Frieren from the First-Class exam dungeon. She copies Frieren's magic perfectly, which
 * includes Frieren's one opening: her defensive magic drops for a moment whenever she attacks. While guarding she
 * is untouchable; she winds up Zoltraak behind a magic circle (dodge sideways), fires, and is exposed until her
 * barrier comes back. Below half health she fires fans of three beams; below a fifth, two volleys in a row.
 */
public class MirrorFrierenEntity extends Monster implements FrierenBoss, ManaSignature, CharacterPoses {
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(MirrorFrierenEntity.class, EntityDataSerializers.BYTE);
    private static final byte GUARD = 0;
    private static final byte CHARGE = 1;
    private static final byte OPEN = 2;
    private static final float BASE_HEALTH = 240.0F;
    private static final float BEAM_DAMAGE = 9.0F;
    private static final double BEAM_RANGE = 36.0D;
    private static final int COLOR = 0x9FD8FF;

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
        Component.translatable("entity.frieren.mirror_frieren"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean scaled;
    private int stateTicks = 60;
    private int volleysLeft;
    private Vec3 aimPoint = Vec3.ZERO;
    private boolean announcedOpening;

    public MirrorFrierenEntity(EntityType<? extends MirrorFrierenEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, BASE_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, BEAM_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, 0.27D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
            .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayersGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, GUARD);
    }

    private byte state() {
        return this.entityData.get(DATA_STATE);
    }

    private void setState(byte state, int ticks) {
        this.entityData.set(DATA_STATE, state);
        this.stateTicks = ticks;
    }

    /** True while her defensive magic is down. */
    public boolean isExposed() {
        return state() == OPEN || (state() == CHARGE && stateTicks <= 8);
    }

    @Override
    public boolean isCastingPose() {
        return state() == CHARGE;
    }

    private int phase() {
        float fraction = this.getHealth() / this.getMaxHealth();
        return fraction < 0.2F ? 3 : fraction < 0.5F ? 2 : 1;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!scaled) {
            scaled = true;
            AuraEntity.applyDifficultyScaling(this, BASE_HEALTH, BEAM_DAMAGE);
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.mirror_frieren.spawn").withStyle(ChatFormatting.AQUA));
        }
        BossBarHelper.update(bossBar, level, this);
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target.distanceToSqr(this) > 48.0D * 48.0D)) {
            this.setTarget(null);
            target = null;
        }
        stateTicks--;
        switch (state()) {
            case GUARD -> tickGuard(level, target);
            case CHARGE -> tickCharge(level, target);
            default -> tickOpen(target);
        }
    }

    private void tickGuard(ServerLevel level, LivingEntity target) {
        if (target == null) {
            this.getNavigation().stop();
            stateTicks = Math.max(stateTicks, 20);
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = this.distanceTo(target);
        if (this.tickCount % 10 == 0) {
            if (distance > 13.0D) {
                this.getNavigation().moveTo(target, 1.0D);
            } else if (distance < 7.0D) {
                Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(this.getRandom().nextBoolean() ? 3.0D : -3.0D);
                Vec3 to = this.position().add(away.scale(6.0D)).add(side);
                this.getNavigation().moveTo(to.x, to.y, to.z, 1.2D);
            } else {
                Vec3 toTarget = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                Vec3 strafe = new Vec3(-toTarget.z, 0.0D, toTarget.x).scale(this.getRandom().nextBoolean() ? 4.0D : -4.0D);
                Vec3 to = this.position().add(strafe);
                this.getNavigation().moveTo(to.x, to.y, to.z, 0.9D);
            }
        }
        if (stateTicks <= 0 && this.hasLineOfSight(target)) {
            volleysLeft = phase() == 3 ? 2 : 1;
            beginCharge(level, target);
        }
    }

    private void beginCharge(ServerLevel level, LivingEntity target) {
        this.getNavigation().stop();
        aimPoint = target.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        int charge = phase() == 1 ? 32 : 24;
        faceAim();
        setState(CHARGE, charge);
        MagicCircleEntity.inFront(level, this, MagicCircleEntity.STYLE_ARCANE, COLOR, phase() == 1 ? 1.1F : 1.5F, charge + 4, 1.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SPELL_CAST.get(), SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    private void tickCharge(ServerLevel level, LivingEntity target) {
        this.getNavigation().stop();
        faceAim();
        if (stateTicks > 0) {
            return;
        }
        fireVolley(level);
        if (--volleysLeft > 0 && target != null && target.isAlive()) {
            beginCharge(level, target);
            return;
        }
        announcedOpening = false;
        setState(OPEN, phase() == 1 ? 34 : 24);
    }

    private void tickOpen(LivingEntity target) {
        this.getNavigation().stop();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (stateTicks <= 0) {
            setState(GUARD, 50 + this.getRandom().nextInt(phase() == 1 ? 40 : 20));
            if (this.level() instanceof ServerLevel level) {
                BarrierShellEntity.flash(level, this, COLOR, 10);
            }
        }
    }

    /** Turns body, head and pitch toward the aim point so the magic circle and the beam line up. */
    private void faceAim() {
        Vec3 eye = this.getEyePosition();
        Vec3 d = aimPoint.subtract(eye);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(d.y, horizontal) * Mth.RAD_TO_DEG));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setXRot(pitch);
    }

    private void fireVolley(ServerLevel level) {
        Vec3 from = this.getEyePosition();
        Vec3 direction = aimPoint.subtract(from).normalize();
        fireBeam(level, from, direction);
        if (phase() >= 2) {
            fireBeam(level, from, direction.yRot(0.22F));
            fireBeam(level, from, direction.yRot(-0.22F));
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.HOSTILE, 1.4F, 0.95F);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void fireBeam(ServerLevel level, Vec3 from, Vec3 direction) {
        Vec3 wanted = from.add(direction.scale(BEAM_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 to = blockHit.getType() == HitResult.Type.MISS ? wanted : blockHit.getLocation();
        List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(level, this, from, to, 0.3D,
            target -> target instanceof Player && SpellDamage.canHarm(level, this, target));
        Vec3 end = to;
        if (!hits.isEmpty()) {
            SpellTargeting.EntityHit hit = hits.get(0);
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            SpellDamage.hurt(level, this, this, hit.entity(), damage, ModDamageTypes.ZOLTRAAK, 1.0F, false, null);
            end = hit.location();
        }
        SpellFx.beam(level, from.add(direction.scale(0.8D)), end, COLOR, 0.34F, 12, SpellBeamEntity.STYLE_ZOLTRAAK);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.isCreativePlayer() || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (!isExposed()) {
            BarrierShellEntity.flash(level, this, COLOR, 8);
            if (source.getEntity() instanceof Player player && this.getRandom().nextInt(3) == 0) {
                Messages.actionBar(player, Component.translatable("boss.frieren.mirror_frieren.guarded"));
            }
            return false;
        }
        if (!announcedOpening && source.getEntity() instanceof Player) {
            announcedOpening = true;
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.mirror_frieren.opening").withStyle(ChatFormatting.GOLD));
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossBar.removeAllPlayers();
        if (this.level() instanceof ServerLevel level) {
            SpellFx.burst(level, this.position().add(0.0D, 1.0D, 0.0D), 0xDDEEFF, 2.0F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BARRIER_BREAK.get(), SoundSource.HOSTILE, 1.5F, 0.7F);
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.mirror_frieren.defeated").withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    public int bonusGrowthReward() {
        return 20;
    }

    @Override
    public float trueMana() {
        return 1200.0F;
    }

    @Override
    public int concealLevel() {
        return 10;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
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
