package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.render.character.CharacterPoses;
import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared machinery for the 2.0 demon bosses (the Demon King, Macht, Aura's generals): boss bar, difficulty scaling,
 * health phases, an attack cooldown, telegraphed ground blasts (a magic circle first, the hit after a delay), beams
 * and minions that never outlive their master. Subclasses only describe their attacks.
 */
public abstract class DemonBoss extends Monster implements FrierenBoss, ManaSignature, CharacterPoses {
    private static final EntityDataAccessor<Boolean> DATA_CASTING = SynchedEntityData.defineId(DemonBoss.class, EntityDataSerializers.BOOLEAN);

    protected final ServerBossEvent bossBar;
    private final List<Blast> blasts = new ArrayList<>();
    private final Set<UUID> minions = new HashSet<>();
    private boolean scaled;
    private int phase = 1;
    private int attackCooldown = 60;
    private int castingTicks;

    /** A telegraphed area hit: the circle shows immediately, damage lands when {@code ticks} reaches zero. */
    private static final class Blast {
        final Vec3 at;
        final float radius;
        final float damage;
        final int color;
        int ticks;

        Blast(Vec3 at, float radius, float damage, int color, int ticks) {
            this.at = at;
            this.radius = radius;
            this.damage = damage;
            this.color = color;
            this.ticks = ticks;
        }
    }

    protected DemonBoss(EntityType<? extends DemonBoss> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossBar = new ServerBossEvent(UUID.randomUUID(), type.getDescription(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.setPersistenceRequired();
    }

    // ---------------------------------------------------------------------------------------
    // Description
    // ---------------------------------------------------------------------------------------

    protected abstract float baseHealth();

    protected abstract float baseDamage();

    /** Health fractions at which phases 2 and 3 begin (descending), e.g. {0.6, 0.25}. */
    protected float[] phaseThresholds() {
        return new float[] {0.5F};
    }

    /** One attack, called whenever the cooldown runs out while there is a target. Returns the next cooldown. */
    protected abstract int attack(ServerLevel level, LivingEntity target, int phase);

    /** Called once when a new phase begins. */
    protected void onPhase(ServerLevel level, int phase) {}

    /** Per-tick movement while not casting; the default keeps a ranged duelling distance. */
    protected void move(ServerLevel level, LivingEntity target) {
        keepDistance(target, 7.0D, 13.0D);
    }

    /** Every tick with a target, before movement and attacks (charging attacks, regeneration, ...). */
    protected void tickBoss(ServerLevel level, LivingEntity target) {}

    /** Id used for this boss's messages ({@code boss.frieren.<id>.spawn/.phase2/.phase3/.defeated}). */
    protected abstract String bossId();

    private String messageKey() {
        return FrierenIds.key("boss", bossId());
    }

    // ---------------------------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.hunternado.frieren.entity.ai.HuntPlayersGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CASTING, false);
    }

    @Override
    public boolean isCastingPose() {
        return this.entityData.get(DATA_CASTING);
    }

    protected void setCasting(int ticks) {
        castingTicks = ticks;
        this.entityData.set(DATA_CASTING, ticks > 0);
    }

    protected boolean isCasting() {
        return castingTicks > 0;
    }

    public int phase() {
        return phase;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!scaled) {
            scaled = true;
            AuraEntity.applyDifficultyScaling(this, baseHealth(), baseDamage());
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable(messageKey() + ".spawn").withStyle(ChatFormatting.DARK_RED));
        }
        BossBarHelper.update(bossBar, level, this);
        tickBlasts(level);
        if (this.tickCount % 20 == 0) {
            minions.removeIf(id -> {
                Entity minion = level.getEntity(id);
                return minion == null || !minion.isAlive();
            });
        }
        updatePhase(level);

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target.distanceToSqr(this) > 48.0D * 48.0D)) {
            this.setTarget(null);
            target = null;
        }
        if (castingTicks > 0 && --castingTicks == 0) {
            this.entityData.set(DATA_CASTING, false);
        }
        if (target == null) {
            return;
        }
        tickBoss(level, target);
        if (castingTicks > 0) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        } else {
            move(level, target);
        }
        if (--attackCooldown <= 0 && this.hasLineOfSight(target)) {
            attackCooldown = Math.max(10, attack(level, target, phase));
        }
    }

    private void updatePhase(ServerLevel level) {
        float fraction = this.getHealth() / this.getMaxHealth();
        float[] thresholds = phaseThresholds();
        int next = 1;
        for (float threshold : thresholds) {
            if (fraction < threshold) {
                next++;
            }
        }
        if (next > phase) {
            phase = next;
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable(messageKey() + ".phase" + phase).withStyle(ChatFormatting.RED));
            onPhase(level, phase);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Attack helpers
    // ---------------------------------------------------------------------------------------

    /** Strafes at a duelling distance from the target. */
    protected void keepDistance(LivingEntity target, double min, double max) {
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.tickCount % 10 != 0) {
            return;
        }
        double distance = this.distanceTo(target);
        Vec3 toTarget = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 side = new Vec3(-toTarget.z, 0.0D, toTarget.x).scale(this.getRandom().nextBoolean() ? 4.0D : -4.0D);
        Vec3 to;
        if (distance > max) {
            this.getNavigation().moveTo(target, 1.0D);
            return;
        } else if (distance < min) {
            to = this.position().subtract(toTarget.scale(6.0D)).add(side);
        } else {
            to = this.position().add(side);
        }
        this.getNavigation().moveTo(to.x, to.y, to.z, 1.0D);
    }

    /** Charges straight at the target (melee bosses). */
    protected void chase(LivingEntity target, double speed) {
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.tickCount % 5 == 0) {
            this.getNavigation().moveTo(target, speed);
        }
    }

    /** A circle on the ground now, damage to everything non-demonic inside it {@code delay} ticks later. */
    protected void telegraphBlast(ServerLevel level, Vec3 at, float radius, int delay, float damage, byte style, int color) {
        MagicCircleEntity.onGround(level, at, style, color, radius, delay + 6);
        blasts.add(new Blast(at, radius, damage, color, delay));
    }

    private void tickBlasts(ServerLevel level) {
        Iterator<Blast> it = blasts.iterator();
        while (it.hasNext()) {
            Blast blast = it.next();
            if (--blast.ticks > 0) {
                continue;
            }
            it.remove();
            for (LivingEntity victim : SpellTargeting.livingInRadius(level, this, blast.at.add(0.0D, 0.5D, 0.0D), blast.radius,
                candidate -> isFoe(level, candidate))) {
                SpellDamage.hurt(level, this, this, victim, blast.damage, ModDamageTypes.DEMON_MAGIC, 1.5F, false, null);
            }
            SpellFx.burst(level, blast.at.add(0.0D, 0.4D, 0.0D), blast.color, blast.radius);
            SpellFx.wisps(level, blast.at, blast.color, blast.radius * 0.6F);
        }
    }

    /** Fires a beam from the eyes along {@code direction}; hits the first foe, which is returned (or null). */
    protected @Nullable LivingEntity fireBeam(ServerLevel level, Vec3 direction, double range, float damage, int color, float width,
                                              ResourceKey<DamageType> type) {
        Vec3 from = this.getEyePosition();
        Vec3 wanted = from.add(direction.normalize().scale(range));
        BlockHitResult blockHit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 to = blockHit.getType() == HitResult.Type.MISS ? wanted : blockHit.getLocation();
        List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(level, this, from, to, 0.3D + width * 0.4D,
            candidate -> isFoe(level, candidate));
        Vec3 end = to;
        LivingEntity hit = null;
        if (!hits.isEmpty()) {
            hit = hits.get(0).entity();
            SpellDamage.hurt(level, this, this, hit, damage, type, 1.5F, false, null);
            end = hits.get(0).location();
        }
        SpellFx.beam(level, from.add(direction.normalize().scale(0.8D)), end, color, width, 12, SpellBeamEntity.STYLE_DEMON);
        return hit;
    }

    /** Turns to face a point (body, head and pitch) so magic circles and beams line up. */
    protected void faceTowards(Vec3 point) {
        Vec3 d = point.subtract(this.getEyePosition());
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Math.atan2(d.z, d.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(d.y, horizontal) * (180.0D / Math.PI));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setXRot(pitch);
    }

    /** Spawns a minion near the boss that targets {@code target}; at most {@code cap} alive at once. */
    protected void summonMinion(ServerLevel level, EntityType<? extends Mob> type, LivingEntity target, int cap) {
        if (minions.size() >= cap) {
            return;
        }
        Mob minion = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (minion == null) {
            return;
        }
        double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
        Vec3 at = this.position().add(Math.cos(angle) * 3.0D, 0.0D, Math.sin(angle) * 3.0D);
        minion.snapTo(at.x, this.getY(), at.z, this.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(minion)) {
            minion.snapTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        }
        minion.setPersistenceRequired();
        minion.setTarget(target);
        if (level.addFreshEntity(minion)) {
            minions.add(minion.getUUID());
            MagicCircleEntity.onGround(level, minion.position(), MagicCircleEntity.STYLE_DEMON, 0xC0203A, 1.2F, 30);
        }
    }

    /** Players and their allies; never other demons. */
    protected boolean isFoe(ServerLevel level, LivingEntity candidate) {
        return !(candidate instanceof DemonEntity) && !(candidate instanceof DemonBoss)
            && SpellDamage.canHarm(level, this, candidate);
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable(messageKey() + ".defeated").withStyle(ChatFormatting.GOLD));
            SpellFx.burst(level, this.position().add(0.0D, 1.0D, 0.0D), 0xC0203A, 2.5F);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel level) {
            for (UUID id : minions) {
                Entity minion = level.getEntity(id);
                if (minion != null) {
                    minion.discard();
                }
            }
            minions.clear();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Scaled", scaled);
        output.putInt("Phase", phase);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        scaled = input.getBooleanOr("Scaled", false);
        phase = Math.max(1, input.getIntOr("Phase", 1));
    }
}
