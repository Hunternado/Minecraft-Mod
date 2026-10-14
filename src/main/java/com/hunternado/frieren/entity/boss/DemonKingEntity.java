package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.entity.npc.CompanionEntity;
import com.hunternado.frieren.entity.npc.FrierenEntity;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Hostility;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Demon King, whom Himmel's party defeated, and who wields the Mythic magic his grimoires teach.
 * <ul>
 *   <li><b>Sovereign Barrier</b>: alone, no mortal touches him. Unless Frieren, Fern and Stark all stand within
 *   40 blocks of him, he takes a twentieth of every blow and his near-endless mana mends him (4 health a second).
 *   With the party there, the barrier breaks and the fight is real (Frieren's Zoltraak hits him three times as hard,
 *   and strips his Sovereign Ward).</li>
 *   <li><b>Annihilation</b>: a five-second charge, its line shown as a thin crimson thread, then a colossal beam.
 *   Deal him 80 damage while he charges and the spell shatters, leaving him stunned.</li>
 *   <li><b>The Black Sun</b>: three telegraphed rings of blasts around his target.</li>
 *   <li><b>Dominion</b>: summons soldiers and turns every monster around against you.</li>
 *   <li><b>Sovereign Ward</b> (from phase 2): six seconds in which only a fifth of the damage gets through.</li>
 *   <li>Dark Zoltraak volleys, crimson rain, and from a quarter health he blinks around the field.</li>
 * </ul>
 */
public class DemonKingEntity extends DemonBoss {
    private static final int COLOR = 0xB01030;
    private static final double PARTY_RANGE = 40.0D;
    private static final int ANNIHILATION_CHARGE = 100;
    private static final float ANNIHILATION_BREAK = 80.0F;

    private int chargeTicks;
    private int volleys;
    private Vec3 aim = Vec3.ZERO;
    private int annihilationTicks;
    private float annihilationDamage;
    private Vec3 annihilationAim = Vec3.ZERO;
    private int stunTicks;
    private int wardCooldown = 400;
    private boolean partyPresent;
    private boolean partyAnnounced;
    private int untouchableNotice;

    public DemonKingEntity(EntityType<? extends DemonKingEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1500.0D)
            .add(Attributes.ATTACK_DAMAGE, 18.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.24D)
            .add(Attributes.ARMOR, 12.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override protected float baseHealth() { return 1500.0F; }
    @Override protected float baseDamage() { return 18.0F; }
    @Override protected float[] phaseThresholds() { return new float[] {0.6F, 0.25F}; }
    @Override protected String bossId() { return "demon_king"; }
    @Override public int bonusGrowthReward() { return 100; }
    @Override public float trueMana() { return 20000.0F; }
    @Override public int concealLevel() { return 0; }
    @Override public byte senseKind() { return 2; }

    // ---------------------------------------------------------------------------------------
    // The Sovereign Barrier
    // ---------------------------------------------------------------------------------------

    /** Frieren, Fern and Stark, all alive and within range: the only thing his barrier yields to. */
    private boolean findParty(ServerLevel level) {
        AABB box = new AABB(this.blockPosition()).inflate(PARTY_RANGE);
        boolean frieren = !level.getEntitiesOfClass(FrierenEntity.class, box, LivingEntity::isAlive).isEmpty();
        boolean fern = false;
        boolean stark = false;
        for (CompanionEntity companion : level.getEntitiesOfClass(CompanionEntity.class, box, LivingEntity::isAlive)) {
            fern |= companion.role() == CompanionEntity.Role.FERN;
            stark |= companion.role() == CompanionEntity.Role.STARK;
        }
        return frieren && fern && stark;
    }

    public boolean barrierUp() {
        return !partyPresent;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float taken = amount;
        if (!partyPresent) {
            taken *= 0.05F;
            SpellFx.hexagon(level, this.position().add(0.0D, this.getBbHeight() * 0.55D, 0.0D), COLOR, 1.6F);
            if (source.getEntity() instanceof ServerPlayer player && this.tickCount - untouchableNotice > 100) {
                untouchableNotice = this.tickCount;
                Messages.actionBar(player, Component.translatable("message.frieren.demon_king_untouchable").withStyle(ChatFormatting.DARK_RED));
            }
        }
        boolean hurt = super.hurtServer(level, source, taken);
        if (hurt && annihilationTicks > 0) {
            annihilationDamage += taken;
            if (annihilationDamage >= ANNIHILATION_BREAK) {
                annihilationTicks = 0;
                stunTicks = 60;
                setCasting(0);
                SpellFx.burst(level, this.getEyePosition(), COLOR, 3.0F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BARRIER_BREAK.get(), SoundSource.HOSTILE, 2.0F, 0.5F);
                Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                    Component.translatable("boss.frieren.demon_king.annihilation_broken").withStyle(ChatFormatting.GOLD));
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel level && this.tickCount % 20 == 0) {
            boolean present = findParty(level);
            if (present && !partyAnnounced) {
                partyAnnounced = true;
                Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                    Component.translatable("boss.frieren.demon_king.party").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                SpellFx.burst(level, this.position().add(0.0D, 1.5D, 0.0D), 0x9FD8FF, 3.0F);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BARRIER_BREAK.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
            } else if (!present && partyPresent) {
                Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                    Component.translatable("boss.frieren.demon_king.barrier_returns").withStyle(ChatFormatting.DARK_RED));
                partyAnnounced = false;
            }
            partyPresent = present;
            if (!partyPresent && !this.isDeadOrDying() && this.getHealth() < this.getMaxHealth()) {
                this.heal(4.0F);                                   // near-endless mana: alone, he simply mends
            }
        }
        super.aiStep();
    }

    // ---------------------------------------------------------------------------------------
    // Attacks
    // ---------------------------------------------------------------------------------------

    @Override
    protected void move(ServerLevel level, LivingEntity target) {
        if (stunTicks > 0 || annihilationTicks > 0) {
            this.getNavigation().stop();
            return;
        }
        keepDistance(target, 8.0D, 15.0D);
    }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (stunTicks > 0) {
            stunTicks--;
            return;
        }
        if (annihilationTicks > 0) {
            tickAnnihilation(level, target);
            return;
        }
        if (phase() >= 2 && --wardCooldown <= 0) {
            wardCooldown = 400;
            this.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.SOVEREIGN), 120, 0));
            SpellFx.shell(level, this, COLOR);
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.demon_king.ward").withStyle(ChatFormatting.RED));
        }
        if (chargeTicks <= 0) {
            return;
        }
        faceTowards(aim);
        if (--chargeTicks > 0) {
            return;
        }
        Vec3 direction = aim.subtract(this.getEyePosition()).normalize();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        fireBeam(level, direction, 40.0D, damage, COLOR, 0.7F, ModDamageTypes.DEMON_MAGIC);
        if (phase() >= 2) {
            fireBeam(level, direction.yRot(0.25F), 40.0D, damage * 0.7F, COLOR, 0.5F, ModDamageTypes.DEMON_MAGIC);
            fireBeam(level, direction.yRot(-0.25F), 40.0D, damage * 0.7F, COLOR, 0.5F, ModDamageTypes.DEMON_MAGIC);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.HOSTILE, 1.6F, 0.6F);
        if (--volleys > 0) {
            beginCharge(level, target, 14);
        }
    }

    private void beginCharge(ServerLevel level, LivingEntity target, int ticks) {
        aim = target.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        chargeTicks = ticks;
        setCasting(ticks + 2);
        faceTowards(aim);
        MagicCircleEntity.inFront(level, this, MagicCircleEntity.STYLE_DEMON, COLOR, 1.8F, ticks + 4, 1.4F);
    }

    /** Annihilation: tracks its target for three seconds, then locks; a thin thread shows the line it will fire along. */
    private void beginAnnihilation(ServerLevel level, LivingEntity target) {
        annihilationTicks = ANNIHILATION_CHARGE;
        annihilationDamage = 0.0F;
        annihilationAim = target.getEyePosition();
        setCasting(ANNIHILATION_CHARGE + 4);
        MagicCircleEntity.inFront(level, this, MagicCircleEntity.STYLE_DEMON, COLOR, 3.2F, ANNIHILATION_CHARGE + 6, 1.6F);
        MagicCircleEntity.onGround(level, this.position(), MagicCircleEntity.STYLE_DEMON, COLOR, 4.0F, ANNIHILATION_CHARGE + 6);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.CHARGE.get(), SoundSource.HOSTILE, 2.0F, 0.5F);
        Messages.nearby(level, this.position(), BossBarHelper.RANGE,
            Component.translatable("boss.frieren.demon_king.annihilation").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    private void tickAnnihilation(ServerLevel level, LivingEntity target) {
        annihilationTicks--;
        if (annihilationTicks > 40) {
            annihilationAim = target.getEyePosition();
        }
        faceTowards(annihilationAim);
        Vec3 from = this.getEyePosition();
        Vec3 direction = annihilationAim.subtract(from).normalize();
        if (annihilationTicks % 8 == 0) {
            SpellFx.beam(level, from.add(direction.scale(1.2D)), from.add(direction.scale(64.0D)), COLOR, 0.06F, 8, SpellBeamEntity.STYLE_DEMON);
            SpellFx.wisps(level, from.add(direction.scale(1.6D)), COLOR, 1.2F);
        }
        if (annihilationTicks > 0) {
            return;
        }
        Vec3 wanted = from.add(direction.scale(64.0D));
        BlockHitResult blockHit = level.clip(new ClipContext(from, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 to = blockHit.getType() == HitResult.Type.MISS ? wanted : blockHit.getLocation();
        float damage = (float) (50.0D * FrierenConfig.bossDamageMultiplier());
        for (SpellTargeting.EntityHit hit : SpellTargeting.entitiesOnLine(level, this, from, to, 1.4D, candidate -> isFoe(level, candidate))) {
            SpellDamage.hurt(level, this, this, hit.entity(), damage, ModDamageTypes.ANNIHILATION, 10.0F, false, null);
        }
        SpellFx.beam(level, from.add(direction.scale(1.2D)), to, COLOR, 2.2F, 26, SpellBeamEntity.STYLE_DEMON);
        SpellFx.beam(level, from.add(direction.scale(1.2D)), to, 0xFFE0E0, 0.7F, 20, SpellBeamEntity.STYLE_ZOLTRAAK);
        SpellFx.burst(level, to, COLOR, 4.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ANNIHILATION.get(), SoundSource.HOSTILE, 2.5F, 0.5F);
        setCasting(0);
    }

    @Override
    protected int attack(ServerLevel level, LivingEntity target, int phase) {
        if (chargeTicks > 0 || annihilationTicks > 0 || stunTicks > 0) {
            return 10;
        }
        if (phase >= 3 && this.getRandom().nextFloat() < 0.4F) {
            blink(level, target);
        }
        switch (this.getRandom().nextInt(phase >= 2 ? 6 : 5)) {
            case 0 -> {
                volleys = phase >= 3 ? 3 : 1;
                beginCharge(level, target, phase >= 3 ? 18 : 28);
            }
            case 1 -> {
                // Crimson rain: blasts on and around the target.
                Vec3 center = target.position();
                telegraphBlast(level, center, 2.2F, 30, 10.0F, MagicCircleEntity.STYLE_DEMON, COLOR);
                for (int i = 0; i < 4 + phase; i++) {
                    double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
                    double r = 2.5D + this.getRandom().nextDouble() * 4.0D;
                    telegraphBlast(level, center.add(Math.cos(angle) * r, 0.0D, Math.sin(angle) * r), 2.0F, 30 + i * 3, 9.0F,
                        MagicCircleEntity.STYLE_DEMON, COLOR);
                }
                setCasting(20);
            }
            case 2 -> dominion(level, target);
            case 3 -> blackSun(level, target);
            case 4 -> beginAnnihilation(level, target);
            default -> {
                volleys = 2;
                beginCharge(level, target, 20);
            }
        }
        return phase >= 3 ? 40 : phase == 2 ? 55 : 70;
    }

    /** The Black Sun, his own spell: three rolling rings of demonic blasts around the target. */
    private void blackSun(ServerLevel level, LivingEntity target) {
        SpellAreaEntity.spawn(level, this, target.position(), SpellAreaEntity.Mode.BLACK_SUN, 12.0F, 46,
            (float) (12.0D * FrierenConfig.bossDamageMultiplier()), COLOR);
        SpellFx.wisps(level, this.position().add(0.0D, 2.5D, 0.0D), COLOR, 3.0F);
        setCasting(30);
    }

    /** Dominion: soldiers rise, and every monster nearby turns on the nearest player. */
    private void dominion(ServerLevel level, LivingEntity target) {
        summonMinion(level, ModEntities.DEMON_SOLDIER.get(), target, 5);
        summonMinion(level, phase() >= 2 ? ModEntities.DEMON_MAGE.get() : ModEntities.DEMON_SOLDIER.get(), target, 5);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(this.blockPosition()).inflate(24.0D),
                mob -> mob != this && mob.isAlive() && Hostility.isHostile(mob) && !(mob instanceof FrierenBoss))) {
            Player nearest = level.getNearestPlayer(mob, 32.0D);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                mob.setTarget(nearest);
            }
        }
        SpellFx.ring(level, this.position().add(0.0D, 0.1D, 0.0D), COLOR, 12.0F);
        setCasting(16);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.4F, 0.6F);
        Messages.nearby(level, this.position(), BossBarHelper.RANGE,
            Component.translatable("boss.frieren.demon_king.dominion").withStyle(ChatFormatting.DARK_RED));
    }

    /** Phase 3: vanish and reappear beside the target. */
    private void blink(ServerLevel level, LivingEntity target) {
        Vec3 from = this.position();
        double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
        Vec3 to = target.position().add(Math.cos(angle) * 6.0D, 0.0D, Math.sin(angle) * 6.0D);
        if (this.randomTeleport(to.x, to.y, to.z, false)) {
            SpellFx.wisps(level, from.add(0.0D, 1.0D, 0.0D), COLOR, 1.5F);
            SpellFx.burst(level, this.position().add(0.0D, 1.0D, 0.0D), COLOR, 1.5F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TELEPORT.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }
}
