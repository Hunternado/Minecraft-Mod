package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.render.character.CharacterPoses;
import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.WanderGoal;
import com.hunternado.frieren.entity.boss.BossBarHelper;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellManager;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Ground;
import com.hunternado.frieren.util.Hostility;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Übel, the First-Class examinee who cuts anything she can imagine being cut.
 * <ul>
 *   <li><b>Wild</b>: wanders near Second-Class (or better) mages, neutral. Talk to her and she teases you; hit her and
 *   it is a duel (boss bar, her Reelseiden up close, cutting waves at range, a dash when you run).</li>
 *   <li><b>The duel cannot kill her</b>: at a fifth of her health she laughs, yields and decides she likes you. She
 *   writes her Reelseiden into a grimoire for you, hands you her spare staff, and sticks to you from then on (this is
 *   also the duel Serie wants to hear about before she creates a spell for you).</li>
 *   <li><b>Companion</b> (like a tamed wolf, but clingier): follows you, teleports after you when you get far, attacks
 *   whatever hurts you or whatever you attack, and hostile monsters near you. Sneak and right-click to make her stay
 *   or follow again. She never attacks players. If she is "killed" she retreats instead and comes back a day later.</li>
 * </ul>
 */
public class UbelEntity extends PathfinderMob implements ManaSignature, CharacterPoses {
    private static final float YIELD_FRACTION = 0.2F;
    /** Returns of companions whose owner was offline when they retreated (applied on the owner's next check). */
    private static final Map<UUID, Long> PENDING_RETURNS = new ConcurrentHashMap<>();
    private static final int SLASH_RANGE = 6;
    private static final int WAVE_RANGE = 18;

    private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.frieren.ubel"),
        BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);

    private @Nullable UUID ownerId;
    private @Nullable UUID duelistId;
    private boolean staying;
    private int attackCooldown = 40;
    private int dashCooldown = 80;
    private int chatterCooldown = 900;
    private int castingTicks;

    public UbelEntity(EntityType<? extends UbelEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 160.0D)
            .add(Attributes.ATTACK_DAMAGE, 9.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.32D)
            .add(Attributes.FOLLOW_RANGE, 32.0D)
            .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new WanderGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new IdleLookGoal(this, 10.0F));
    }

    // ---------------------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------------------

    public boolean isTamed() {
        return ownerId != null;
    }

    public @Nullable UUID ownerId() {
        return ownerId;
    }

    public void setOwner(@Nullable UUID owner) {
        this.ownerId = owner;
    }

    private @Nullable ServerPlayer owner(ServerLevel level) {
        return ownerId != null && level.getPlayerByUUID(ownerId) instanceof ServerPlayer player ? player : null;
    }

    @Override
    public boolean isCastingPose() {
        return this.isAggressive() || castingTicks > 0;
    }

    @Override
    public float trueMana() {
        return 900.0F;
    }

    @Override
    public int concealLevel() {
        return 3;
    }

    @Override
    public byte senseKind() {
        return 2;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---------------------------------------------------------------------------------------
    // Talking, staying, the duel
    // ---------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(this.level() instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (isTamed() && player.getUUID().equals(ownerId)) {
            if (player.isShiftKeyDown()) {
                staying = !staying;
                this.getNavigation().stop();
                say(serverPlayer, staying ? "stay" : "follow");
            } else {
                say(serverPlayer, "clingy." + this.getRandom().nextInt(6));
            }
        } else if (isTamed()) {
            say(serverPlayer, "taken");
        } else if (duelistId != null) {
            say(serverPlayer, "dueling");
        } else {
            say(serverPlayer, "wild." + this.getRandom().nextInt(4));
        }
        return InteractionResult.SUCCESS;
    }

    private void say(ServerPlayer player, String key) {
        Component line = Component.translatable(FrierenIds.key("npc", "ubel." + key));
        Messages.chat(player, Component.literal("<").append(this.getDisplayName()).append("> ").append(line.copy().withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (isTamed()) {
            if (attacker instanceof Player) {
                return false;                                      // she will not fight her own side
            }
            if (amount >= this.getHealth() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                retreat(level);
                return true;
            }
            return super.hurtServer(level, source, amount);
        }
        if (attacker instanceof ServerPlayer player && !player.isCreative()) {
            if (duelistId == null) {
                beginDuel(level, player);
            }
            if (!player.getUUID().equals(duelistId)) {
                return false;                                      // one opponent at a time
            }
            float floor = this.getMaxHealth() * YIELD_FRACTION;
            boolean hurt = super.hurtServer(level, source, Math.min(amount, Math.max(0.0F, this.getHealth() - floor)));
            if (this.getHealth() <= floor + 0.5F) {
                yieldTo(level, player);
            }
            return hurt;
        }
        return super.hurtServer(level, source, amount);
    }

    private void beginDuel(ServerLevel level, ServerPlayer player) {
        duelistId = player.getUUID();
        this.setTarget(player);
        say(player, "duel_start");
        bossBar.addPlayer(player);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    private void endDuel() {
        duelistId = null;
        bossBar.removeAllPlayers();
        this.setTarget(null);
        this.setAggressive(false);
    }

    /** She lost, laughs about it and decides the mage who beat her is interesting. */
    private void yieldTo(ServerLevel level, ServerPlayer player) {
        endDuel();
        ownerId = player.getUUID();
        staying = false;
        this.setHealth(this.getMaxHealth() * 0.5F);
        SpellFx.petals(level, this.position(), 0x3A8A6A, 1.5F);
        say(player, "yield");
        MagicData data = MagicData.get(player);
        if (data != null) {
            data.setUbelDefeated(true);
            data.setUbelReturnAt(0L);
        }
        SpellDefinition reelseiden = SpellManager.server(FrierenIds.id("ubel_reelseiden"));
        if (reelseiden != null) {
            give(player, GrimoireItem.forSpell(reelseiden));
            Messages.chat(player, Component.translatable("npc.frieren.ubel.gift_grimoire").withStyle(ChatFormatting.GREEN));
            if (data != null && !data.rank().canLearn(reelseiden.rarity())) {
                Messages.chat(player, Component.translatable("npc.frieren.ubel.gift_later", MageRank.FIRST_CLASS.displayName())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        give(player, new ItemStack(ModItems.UBEL_STAFF.get()));
        ModTriggers.milestone(player, "ubel_companion");
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** A companion is never killed: she slips away wounded and finds her mage again a day later. */
    private void retreat(ServerLevel level) {
        ServerPlayer owner = owner(level);
        if (owner != null) {
            MagicData data = MagicData.get(owner);
            if (data != null) {
                data.setUbelReturnAt(level.getGameTime() + 24000L);
            }
            say(owner, "retreat");
        } else if (ownerId != null) {
            PENDING_RETURNS.put(ownerId, level.getGameTime() + 24000L);   // the owner is offline: settle it on their return
        }
        SpellFx.burst(level, this.position().add(0.0D, 1.0D, 0.0D), 0x3A8A6A, 1.2F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.TELEPORT.get(), SoundSource.NEUTRAL, 1.0F, 1.2F);
        this.discard();
    }

    // ---------------------------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (castingTicks > 0) {
            castingTicks--;
        }
        if (duelistId != null) {
            tickDuel(level);
            BossBarHelper.update(bossBar, level, this);
            return;
        }
        if (isTamed()) {
            tickCompanion(level);
        }
    }

    private void tickDuel(ServerLevel level) {
        Player duelist = level.getPlayerByUUID(duelistId);
        if (!(duelist instanceof ServerPlayer player) || !player.isAlive() || player.distanceToSqr(this) > 48.0D * 48.0D) {
            if (duelist instanceof ServerPlayer gone) {
                say(gone, "duel_bored");
            }
            endDuel();
            this.heal(this.getMaxHealth());
            return;
        }
        fight(level, player);
    }

    private void tickCompanion(ServerLevel level) {
        ServerPlayer owner = owner(level);
        if (owner == null) {
            this.getNavigation().stop();
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || !isFoe(level, target) || target.distanceToSqr(owner) > 24.0D * 24.0D)) {
            this.setTarget(null);
            target = null;
        }
        if (target == null && this.tickCount % 10 == 0) {
            target = pickTarget(level, owner);
            this.setTarget(target);
        }
        if (target != null) {
            fight(level, target);
        } else {
            this.setAggressive(false);
            if (!staying) {
                follow(level, owner);
            }
        }
        if (--chatterCooldown <= 0) {
            chatterCooldown = 900 + this.getRandom().nextInt(1200);
            if (owner.distanceToSqr(this) < 12.0D * 12.0D) {
                say(owner, "clingy." + this.getRandom().nextInt(6));
            }
        }
    }

    /** Like a wolf: whoever hurt the owner, whatever the owner is fighting, else the nearest hostile near the owner. */
    private @Nullable LivingEntity pickTarget(ServerLevel level, ServerPlayer owner) {
        for (LivingEntity candidate : new LivingEntity[] {owner.getLastHurtByMob(), owner.getLastHurtMob()}) {
            if (candidate != null && candidate.isAlive() && isFoe(level, candidate) && candidate.distanceToSqr(owner) < 16.0D * 16.0D) {
                return candidate;
            }
        }
        List<LivingEntity> hostiles = SpellTargeting.livingInRadius(level, this, owner.position(), 10.0D,
            candidate -> Hostility.isHostile(candidate) && isFoe(level, candidate));
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity hostile : hostiles) {
            double distance = hostile.distanceToSqr(this);
            if (distance < best) {
                best = distance;
                nearest = hostile;
            }
        }
        return nearest;
    }

    /** Never players (outside a duel), the owner's pets, NPCs or her own kind. */
    private boolean isFoe(ServerLevel level, LivingEntity candidate) {
        if (candidate == this || !candidate.isAlive() || candidate instanceof FrierenNpcBase || candidate instanceof UbelEntity) {
            return false;
        }
        if (candidate instanceof Player player) {
            return duelistId != null && player.getUUID().equals(duelistId);
        }
        if (candidate instanceof TamableAnimal pet && pet.getOwner() != null && ownerId != null && ownerId.equals(pet.getOwner().getUUID())) {
            return false;
        }
        return true;
    }

    private void follow(ServerLevel level, ServerPlayer owner) {
        double distance = this.distanceToSqr(owner);
        if (distance > 24.0D * 24.0D) {
            Vec3 spot = Ground.standable(level, owner.position().subtract(owner.getLookAngle().multiply(2.0D, 0.0D, 2.0D)));
            if (spot != null) {
                this.teleportTo(spot.x, spot.y, spot.z);
                this.getNavigation().stop();
                if (this.getRandom().nextInt(3) == 0) {
                    say(owner, "caught_up");
                }
            }
        } else if (distance > 4.0D * 4.0D) {
            this.getNavigation().moveTo(owner, 1.2D);
        } else {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(owner, 20.0F, 20.0F);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Fighting
    // ---------------------------------------------------------------------------------------

    private void fight(ServerLevel level, LivingEntity target) {
        this.setAggressive(true);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = this.distanceTo(target);
        if (distance > SLASH_RANGE - 1.5D) {
            this.getNavigation().moveTo(target, 1.25D);
        } else {
            this.getNavigation().stop();
        }
        if (--dashCooldown <= 0 && distance > 10.0D && distance < 24.0D) {
            dashCooldown = 120;
            Vec3 toward = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(toward.x * 1.4D, 0.35D, toward.z * 1.4D);
            this.needsSync = true;
            SpellFx.wisps(level, this.position().add(0.0D, 0.8D, 0.0D), 0x3A8A6A, 0.6F);
        }
        if (--attackCooldown > 0 || !this.hasLineOfSight(target)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (distance <= SLASH_RANGE) {
            reelseiden(level, target, damage);
            attackCooldown = 28;
        } else if (distance <= WAVE_RANGE) {
            cuttingWave(level, target, damage * 0.8F);
            attackCooldown = 46;
        }
    }

    /** Reelseiden up close: a wide arc that goes through armour and nearly any barrier. */
    private void reelseiden(ServerLevel level, LivingEntity target, float damage) {
        castingTicks = 8;
        Vec3 eye = this.getEyePosition();
        Vec3 look = target.getEyePosition().subtract(eye).normalize();
        for (LivingEntity victim : SpellTargeting.livingInRadius(level, this, eye, SLASH_RANGE, candidate -> isFoe(level, candidate))) {
            Vec3 to = victim.getEyePosition().subtract(eye).normalize();
            if (to.dot(look) > Math.cos(Math.toRadians(55.0D))) {
                SpellDamage.hurt(level, this, this, victim, damage, ModDamageTypes.CUTTING, 20.0F, false, null);
            }
        }
        SpellFx.cone(level, eye, look, 0x3A8A6A, SLASH_RANGE, 1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 1.0F, 1.3F);
    }

    /** At range: a thin cutting wave along the line to the target. */
    private void cuttingWave(ServerLevel level, LivingEntity target, float damage) {
        castingTicks = 10;
        Vec3 from = this.getEyePosition();
        Vec3 to = target.getEyePosition();
        for (SpellTargeting.EntityHit hit : SpellTargeting.entitiesOnLine(level, this, from, to, 0.5D, candidate -> isFoe(level, candidate))) {
            SpellDamage.hurt(level, this, this, hit.entity(), damage, ModDamageTypes.CUTTING, 10.0F, false, null);
        }
        SpellFx.beam(level, from, to, 0x9ADCC0, 0.15F, 8, SpellBeamEntity.STYLE_LIGHT);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 0.8F, 1.7F);
    }

    // ---------------------------------------------------------------------------------------
    // Spawning and returning
    // ---------------------------------------------------------------------------------------

    /**
     * Once a day: Übel turns up near a Second-Class (or better) mage who has not beaten her yet, and retreated Übels
     * come back to their mages.
     */
    public static void dailyCheck(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            MagicData data = MagicData.get(player);
            if (data == null || player.isSpectator()) {
                continue;
            }
            Long pending = PENDING_RETURNS.remove(player.getUUID());
            if (pending != null && data.ubelReturnAt() <= 0L) {
                data.setUbelReturnAt(pending);
            }
            if (!data.ubelDefeated() && data.rank().atLeast(MageRank.SECOND_CLASS) && level.getRandom().nextInt(3) == 0
                    && level.getEntitiesOfClass(UbelEntity.class, new AABB(player.blockPosition()).inflate(160.0D)).isEmpty()) {
                spawnNear(level, player, null);
            }
        }
    }

    /** Every few seconds per player: a retreated companion whose day is up comes back. */
    public static void tickReturn(ServerLevel level, ServerPlayer player, MagicData data) {
        Long pending = PENDING_RETURNS.remove(player.getUUID());
        if (pending != null && data.ubelReturnAt() <= 0L) {
            data.setUbelReturnAt(pending);
        }
        if (data.ubelReturnAt() <= 0L || level.getGameTime() < data.ubelReturnAt()) {
            return;
        }
        data.setUbelReturnAt(0L);
        UbelEntity ubel = spawnNear(level, player, player.getUUID());
        if (ubel != null) {
            ubel.say(player, "returned");
        }
    }

    private static @Nullable UbelEntity spawnNear(ServerLevel level, ServerPlayer player, @Nullable UUID owner) {
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        int distance = owner != null ? 4 : 24 + level.getRandom().nextInt(16);
        BlockPos column = player.blockPosition().offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
        BlockPos ground = owner != null ? player.blockPosition() : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (owner == null && !level.getBlockState(ground.below()).isSolid()) {
            return null;
        }
        UbelEntity ubel = ModEntities.UBEL.get().create(level, EntitySpawnReason.EVENT);
        if (ubel == null) {
            return null;
        }
        Vec3 at = owner != null ? player.position().add(Math.cos(angle) * 1.5D, 0.0D, Math.sin(angle) * 1.5D) : Vec3.atBottomCenterOf(ground);
        ubel.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        ubel.setOwner(owner);
        level.addFreshEntity(ubel);
        return ubel;
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
        output.putBoolean("Staying", staying);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String owner = input.getStringOr("Owner", "");
        try {
            ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            ownerId = null;
        }
        staying = input.getBooleanOr("Staying", false);
    }
}
