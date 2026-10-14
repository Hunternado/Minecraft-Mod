package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.client.render.character.CharacterPoses;
import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.combat.SpellDamageSource;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.ai.FollowLeaderGoal;
import com.hunternado.frieren.entity.ai.HuntHostilesGoal;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.RangedSpellGoal;
import com.hunternado.frieren.entity.ai.WanderGoal;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Ground;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Frieren the Slayer, travelling with Fern and Stark. A rare encounter: she trades the everyday spells she collects
 * and conceals her mana so well that detection barely notices her.
 * <ul>
 *   <li><b>She hunts demons.</b> Anything hostile near her gets her Zoltraak, and demons get the version that made
 *   her "the Slayer": three times the damage, and it strips the protective magic off its target. Her defensive magic
 *   halves the spells and arrows that reach her.</li>
 *   <li><b>Her party can travel with you.</b> A First-Class mage who sneaks and talks to her can ask the party to
 *   come along (to face the Demon King, for instance; they will not leave while you lead them). Ask again to let
 *   them go their own way.</li>
 *   <li>If Aura is anywhere close, Frieren goes to settle it (see AuraDuel).</li>
 * </ul>
 */
public class FrierenEntity extends FrierenNpcBase implements RangedSpellGoal.Caster, CharacterPoses {
    private static final long STAY_TICKS = 24000L;
    private static final EntityDataAccessor<Boolean> DATA_REVEALED = SynchedEntityData.defineId(FrierenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CASTING = SynchedEntityData.defineId(FrierenEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID partyLeader;
    private @Nullable Player cachedLeader;
    /** While a scripted scene (the duel with Aura) runs, her own AI stands aside. */
    private boolean scripted;
    private int castingTicks;

    public FrierenEntity(EntityType<? extends FrierenEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FrierenNpcBase.createAttributes()
            .add(Attributes.MAX_HEALTH, 120.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEALED, false);
        builder.define(DATA_CASTING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedSpellGoal(this, this, 1.0D, 6.0D, 22.0D, 30));
        this.goalSelector.addGoal(4, new FollowLeaderGoal(this, this::leader, 1.1D, 6.0D, 3.0D));
        this.goalSelector.addGoal(6, new WanderGoal(this, 0.5D));
        this.goalSelector.addGoal(7, new IdleLookGoal(this, 8.0F));
        this.targetSelector.addGoal(1, new HuntHostilesGoal(this, 22.0D));
    }

    @Override
    protected String npcId() {
        return "frieren";
    }

    @Override
    protected int dialogueLines() {
        return 5;
    }

    @Override
    public float trueMana() {
        return 3000.0F;
    }

    /** Her concealment is the best there is, until she chooses to release it. */
    @Override
    public int concealLevel() {
        return isRevealed() ? 0 : 9;
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
    }

    public void setRevealed(boolean revealed) {
        this.entityData.set(DATA_REVEALED, revealed);
    }

    public void setScripted(boolean scripted) {
        this.scripted = scripted;
        if (scripted) {
            this.setTarget(null);
            this.getNavigation().stop();
        }
    }

    public boolean isScripted() {
        return scripted;
    }

    @Override
    public boolean isCastingPose() {
        return this.entityData.get(DATA_CASTING);
    }

    public void pose(int ticks) {
        castingTicks = ticks;
        this.entityData.set(DATA_CASTING, ticks > 0);
    }

    @Override
    public List<NpcTrades.Offer> offers() {
        return NpcTrades.frieren(this.getUUID().getLeastSignificantBits());
    }

    @Override
    public String tradeTitleKey() {
        return "screen.frieren.trades.frieren";
    }

    // ---------------------------------------------------------------------------------------
    // The party
    // ---------------------------------------------------------------------------------------

    public @Nullable UUID partyLeader() {
        return partyLeader;
    }

    private @Nullable LivingEntity leader() {
        if (partyLeader == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        if (cachedLeader == null || !cachedLeader.isAlive() || !cachedLeader.getUUID().equals(partyLeader)) {
            cachedLeader = level.getPlayerByUUID(partyLeader);
        }
        return cachedLeader;
    }

    /** Fern and Stark, if they are still travelling with her. */
    public List<CompanionEntity> companions(ServerLevel level) {
        return level.getEntitiesOfClass(CompanionEntity.class, new AABB(this.blockPosition()).inflate(64.0D),
            companion -> companion.isFollowing(this));
    }

    @Override
    public void onInteract(ServerPlayer player) {
        if (scripted) {
            return;
        }
        if (!player.isShiftKeyDown()) {
            sayRandomLine(player);
            NpcInteractions.openTrades(player, this);
            return;
        }
        ServerLevel level = player.level();
        MagicData data = MagicData.get(player);
        if (partyLeader != null && partyLeader.equals(player.getUUID())) {
            partyLeader = null;
            long departAt = level.getGameTime() + STAY_TICKS;
            setDepartAt(departAt);
            for (CompanionEntity companion : companions(level)) {
                companion.setDepartAt(departAt);
            }
            say(player, "party_leave");
            return;
        }
        if (partyLeader != null) {
            say(player, "party_taken");
            return;
        }
        if (data == null || data.rank() != MageRank.FIRST_CLASS) {
            say(player, "party_rank");
            return;
        }
        partyLeader = player.getUUID();
        setDepartAt(0L);
        for (CompanionEntity companion : companions(level)) {
            companion.setDepartAt(0L);
        }
        say(player, "party_join");
        ModTriggers.milestone(player, "frieren_party");
    }

    private void say(ServerPlayer player, String key) {
        Messages.chat(player, Component.literal("<").append(this.getDisplayName()).append("> ")
            .append(Component.translatable("npc.frieren.frieren." + key).withStyle(ChatFormatting.WHITE)));
    }

    // ---------------------------------------------------------------------------------------
    // Fighting
    // ---------------------------------------------------------------------------------------

    @Override
    public boolean canCastNow() {
        return !scripted;
    }

    /** The Slayer's Zoltraak: triple damage to demons, and the protective magic comes off with it. */
    @Override
    public void castAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level) || scripted) {
            return;
        }
        boolean demon = SpellDamage.isDemon(target);
        float damage = demon ? 30.0F : 10.0F;
        if (demon) {
            target.removeEffect(ModEffects.holder(ModEffects.SOVEREIGN));
        }
        MagicCircleEntity.inFront(level, this, MagicCircleEntity.STYLE_ARCANE, 0xE8F4FF, 0.9F, 10, 1.0F);
        SpellDamage.hurt(level, this, this, target, damage, ModDamageTypes.ZOLTRAAK, demon ? 4.0F : 1.0F, false, null);
        SpellFx.beam(level, this.getEyePosition(), target.getEyePosition(), 0xE8F4FF, demon ? 0.5F : 0.3F, 12, SpellBeamEntity.STYLE_ZOLTRAAK);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.NEUTRAL, 1.0F, 1.1F);
        pose(12);
    }

    /** Her defensive magic: spells and arrows that reach her lose half their force. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float taken = amount;
        if (source instanceof SpellDamageSource || source.is(DamageTypeTags.IS_PROJECTILE)) {
            taken *= 0.5F;
            SpellFx.shell(level, this, 0x9FD8FF);
        }
        return super.hurtServer(level, source, taken);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (castingTicks > 0 && --castingTicks == 0) {
            this.entityData.set(DATA_CASTING, false);
        }
        if (scripted) {
            this.setTarget(null);
            return;
        }
        LivingEntity leader = leader();
        if (leader != null && this.tickCount % 20 == 0 && this.distanceToSqr(leader) > 32.0D * 32.0D) {
            Vec3 spot = Ground.standable(level, leader.position().subtract(leader.getLookAngle().multiply(2.5D, 0.0D, 2.5D)));
            if (spot != null) {
                this.teleportTo(spot.x, spot.y, spot.z);
                this.getNavigation().stop();
            }
        }
        if (this.tickCount % 40 == 0) {
            AuraDuel.tryBegin(level, this);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Spawning and saving
    // ---------------------------------------------------------------------------------------

    /** Called about once a day: with a 1 in 3 chance, the party shows up near a random player. */
    public static void trySpawnTravellingParty(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() || level.getRandom().nextInt(3) != 0) {
            return;
        }
        ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
        if (!level.getEntitiesOfClass(FrierenEntity.class, new AABB(player.blockPosition()).inflate(160.0D)).isEmpty()) {
            return;
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        int distance = 24 + level.getRandom().nextInt(16);
        BlockPos column = player.blockPosition().offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (!level.getBlockState(ground.below()).isSolid()) {
            return;
        }
        long departAt = level.getGameTime() + STAY_TICKS;
        FrierenEntity frieren = ModEntities.FRIEREN.get().create(level, EntitySpawnReason.EVENT);
        if (frieren == null) {
            return;
        }
        frieren.snapTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, 0.0F, 0.0F);
        frieren.setDepartAt(departAt);
        level.addFreshEntity(frieren);
        spawnCompanion(level, ModEntities.FERN.get(), frieren, ground.offset(1, 0, 0), departAt);
        spawnCompanion(level, ModEntities.STARK.get(), frieren, ground.offset(-1, 0, 0), departAt);
    }

    private static void spawnCompanion(ServerLevel level, EntityType<CompanionEntity> type, FrierenEntity leader, BlockPos pos, long departAt) {
        CompanionEntity companion = type.create(level, EntitySpawnReason.EVENT);
        if (companion == null) {
            return;
        }
        companion.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        companion.setLeader(leader);
        companion.setDepartAt(departAt);
        level.addFreshEntity(companion);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (partyLeader != null) {
            output.putString("PartyLeader", partyLeader.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String leader = input.getStringOr("PartyLeader", "");
        try {
            partyLeader = leader.isEmpty() ? null : UUID.fromString(leader);
        } catch (IllegalArgumentException e) {
            partyLeader = null;
        }
    }
}
