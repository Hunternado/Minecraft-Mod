package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.ai.HuntPlayersGoal;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.RangedSpellGoal;
import com.hunternado.frieren.entity.demon.AuraThrallEntity;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.magic.ManaBoltEntity;
import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Aura the Guillotine, one of the Seven Sages of Destruction.
 * <ol>
 *   <li><b>Command</b> (100–60%): raises headless thralls and fires demon bolts from a distance.</li>
 *   <li><b>Scales of Obedience</b> (60–30%): weighs the nearest player's mana against her own. Release
 *   your concealment during the weighing to reveal your true strength (amplified by concealment mastery).
 *   Win and she is forced to strike herself; lose and you are enthralled.</li>
 *   <li><b>Guillotine</b> (&lt;30%): blink strikes, telegraphed guillotine circles and endless thralls.</li>
 * </ol>
 */
public class AuraEntity extends DemonEntity implements FrierenBoss, RangedSpellGoal.Caster {
    public enum Phase { COMMAND, SCALES, GUILLOTINE }

    private static final float BASE_HEALTH = 300.0F;
    private static final float BASE_MANA = 300.0F;
    private static final int WEIGH_TICKS = 100;

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
        Component.translatable("entity.frieren.aura"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    private Phase phase = Phase.COMMAND;
    private boolean scaled;
    private int thrallCooldown = 60;
    private int scalesCooldown = 80;
    private int guillotineCooldown = 100;
    private int blinkCooldown = 160;
    private int stunTicks;
    private @Nullable UUID weighedPlayer;
    private int weighTicks;
    private boolean weighedWasConcealing;

    public AuraEntity(EntityType<? extends AuraEntity> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, BASE_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, 12.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.ARMOR, 10.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
            .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedSpellGoal(this, this, 1.0D, 5.0D, 18.0D, 30));
        this.goalSelector.addGoal(6, new IdleLookGoal(this, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HuntPlayersGoal(this));
    }

    @Override
    public int bonusGrowthReward() {
        return 40;
    }

    @Override
    protected boolean canDeceive() {
        return false;
    }

    @Override
    protected float maxBarrier() {
        return phase == Phase.GUILLOTINE ? 0.0F : 40.0F;
    }

    private float auraMana() {
        return (float) (BASE_MANA * FrierenConfig.bossHealthMultiplier());
    }

    @Override
    public float trueMana() {
        return auraMana();
    }

    @Override
    public int concealLevel() {
        // Aura never hides her mana: she is proud of it. That pride is her downfall.
        return 0;
    }

    @Override
    public boolean canCastNow() {
        return stunTicks <= 0 && weighedPlayer == null;
    }

    @Override
    public void castAt(LivingEntity target) {
        if (this.level() instanceof ServerLevel level) {
            float damage = (float) (7.0D * FrierenConfig.bossDamageMultiplier());
            ManaBoltEntity.shoot(level, this, target.getEyePosition(), damage, 1.2F, 0xD1475E, ModDamageTypes.DEMON_MAGIC, 1.0F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.DEMON_BOLT.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
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
            applyDifficultyScaling(this, BASE_HEALTH, 12.0F);
        }
        BossBarHelper.update(bossBar, level, this);
        updatePhase(level);

        if (stunTicks > 0) {
            stunTicks--;
            this.setTarget(null);
            this.getNavigation().stop();
            return;
        }
        if (weighedPlayer != null) {
            tickWeighing(level);
            return;
        }

        if (--thrallCooldown <= 0) {
            thrallCooldown = phase == Phase.GUILLOTINE ? 200 : 300;
            summonThralls(level, phase == Phase.COMMAND ? 2 : 1);
        }
        if (phase == Phase.SCALES && --scalesCooldown <= 0) {
            scalesCooldown = 400;
            beginWeighing(level);
        }
        if (phase == Phase.GUILLOTINE) {
            LivingEntity target = this.getTarget();
            if (target != null && --guillotineCooldown <= 0) {
                guillotineCooldown = 120;
                SpellAreaEntity.spawn(level, this, target.position(), SpellAreaEntity.Mode.GUILLOTINE_MARK, 3.0F, 30,
                    (float) (16.0D * FrierenConfig.bossDamageMultiplier()), 0xFF2040);
            }
            if (target != null && --blinkCooldown <= 0) {
                blinkCooldown = 160;
                Vec3 behind = target.position().subtract(target.getLookAngle().scale(2.5D));
                SpellFx.burst(level, this.position().add(0, 1.0D, 0), 0x6A2C70, 1.0F);
                this.teleportTo(behind.x, target.getY(), behind.z);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TELEPORT.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
            }
        }
    }

    private void updatePhase(ServerLevel level) {
        float ratio = this.getHealth() / this.getMaxHealth();
        Phase next = ratio > 0.6F ? Phase.COMMAND : ratio > 0.3F ? Phase.SCALES : Phase.GUILLOTINE;
        if (next != phase) {
            phase = next;
            bossBar.setColor(phase == Phase.GUILLOTINE ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable(FrierenIds.key("aura", "phase." + phase.name().toLowerCase(java.util.Locale.ROOT)))
                    .withStyle(ChatFormatting.DARK_RED));
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.5F, 0.9F);
        }
    }

    private void summonThralls(ServerLevel level, int count) {
        int alive = level.getEntitiesOfClass(AuraThrallEntity.class, new AABB(this.blockPosition()).inflate(32.0D)).size();
        for (int i = 0; i < count && alive < 6; i++, alive++) {
            AuraThrallEntity thrall = ModEntities.AURA_THRALL.get().create(level, EntitySpawnReason.REINFORCEMENT);
            if (thrall == null) {
                return;
            }
            double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
            thrall.snapTo(this.getX() + Math.cos(angle) * 3.0D, this.getY(), this.getZ() + Math.sin(angle) * 3.0D, this.getYRot(), 0.0F);
            thrall.setTarget(this.getTarget());
            level.addFreshEntity(thrall);
            SpellFx.burst(level, thrall.position().add(0, 1.0D, 0), 0x7A1E2C, 0.8F);
        }
    }

    // ---- Scales of Obedience --------------------------------------------------------------

    private void beginWeighing(ServerLevel level) {
        Player nearest = level.getNearestPlayer(this, 20.0D);
        if (!(nearest instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        weighedPlayer = player.getUUID();
        weighTicks = WEIGH_TICKS;
        weighedWasConcealing = data.concealing();
        this.getNavigation().stop();
        bossBar.setName(Component.translatable("boss.frieren.aura.weighing", player.getDisplayName()));
        Messages.nearby(level, this.position(), BossBarHelper.RANGE,
            Component.translatable("boss.frieren.aura.scales", player.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.AURA_SCALES.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
    }

    private void tickWeighing(ServerLevel level) {
        Entity entity = weighedPlayer != null ? level.getEntity(weighedPlayer) : null;
        if (!(entity instanceof ServerPlayer player) || !player.isAlive() || player.distanceToSqr(this) > 40.0D * 40.0D) {
            endWeighing();
            return;
        }
        this.getNavigation().stop();
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);
        if (weighTicks % 20 == 0) {
            SpellFx.scales(level, this.position().add(0, this.getBbHeight() + 0.8D, 0), 0xD4AF37);
            SpellFx.scales(level, player.position().add(0, player.getBbHeight() + 0.8D, 0), 0xD4AF37);
        }
        if (--weighTicks > 0) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            endWeighing();
            return;
        }
        float playerWeight;
        if (data.concealing()) {
            // Still hiding: Aura weighs only what she can perceive.
            playerWeight = ManaHelper.apparentMana(data);
        } else if (weighedWasConcealing) {
            // Released the limiter mid-weighing: centuries of suppression become strength.
            float bonus = data.concealMastery() / 100.0F + (data.hasConcealmentCharm() ? 0.15F : 0.0F);
            playerWeight = data.maxMana() * (1.0F + bonus);
        } else {
            playerWeight = data.maxMana();
        }
        if (playerWeight > auraMana()) {
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.aura.obey", player.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            stunTicks = 100;
            this.hurtServer(level, this.damageSources().magic(), this.getMaxHealth() * 0.4F);
            com.hunternado.frieren.registry.ModTriggers.milestone(player, "aura_obeyed");
        } else {
            Messages.nearby(level, this.position(), BossBarHelper.RANGE,
                Component.translatable("boss.frieren.aura.enthrall", player.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
            player.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.ENTHRALLED), 100, 0));
            data.setEnthralledUntil(level.getGameTime() + 100L);
        }
        endWeighing();
    }

    private void endWeighing() {
        weighedPlayer = null;
        weighTicks = 0;
        bossBar.setName(this.getDisplayName());
    }

    /** Scales health and melee damage from config once, on the first server tick. */
    static void applyDifficultyScaling(LivingEntity boss, float baseHealth, float baseDamage) {
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(baseHealth * FrierenConfig.bossHealthMultiplier());
            boss.setHealth(boss.getMaxHealth());
        }
        AttributeInstance damage = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(baseDamage * FrierenConfig.bossDamageMultiplier());
        }
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
        bossBar.setName(this.getDisplayName());
    }
}
