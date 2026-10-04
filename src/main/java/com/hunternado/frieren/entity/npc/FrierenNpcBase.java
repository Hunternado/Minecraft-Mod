package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.entity.ai.WanderGoal;
import com.hunternado.frieren.mana.ManaSignature;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Base for friendly NPCs: never despawn, cannot be harmed by players, talk when interacted with and may
 * offer trades. Interaction is routed through {@link NpcInteractions}.
 */
public abstract class FrierenNpcBase extends PathfinderMob implements ManaSignature {
    /** Game time after which a temporary NPC (travelling party, Serie) leaves. 0 = permanent. */
    private long departAt;

    protected FrierenNpcBase(EntityType<? extends FrierenNpcBase> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            .add(Attributes.FOLLOW_RANGE, 24.0D)
            .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    /** Whether the NPC wanders. Examiners and Serie stand still. */
    protected boolean wanders() {
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (wanders()) {
            this.goalSelector.addGoal(6, new WanderGoal(this, 0.5D));
        }
        this.goalSelector.addGoal(7, new IdleLookGoal(this, 8.0F));
    }

    /** Server-side interaction (already distance-checked by the caller). */
    public abstract void onInteract(ServerPlayer player);

    /** Trades offered by this NPC; empty for non-traders. Stable per entity (seeded by UUID). */
    public List<NpcTrades.Offer> offers() {
        return List.of();
    }

    public String tradeTitleKey() {
        return "screen.frieren.trades";
    }

    /** Language key prefix for random dialogue lines: {@code npc.frieren.<id>.line.<n>}. */
    protected abstract String npcId();

    protected int dialogueLines() {
        return 3;
    }

    protected void sayRandomLine(ServerPlayer player) {
        Component line = Component.translatable(FrierenIds.key("npc", npcId() + ".line." + this.getRandom().nextInt(dialogueLines())));
        Messages.chat(player, Component.literal("<").append(this.getDisplayName()).append("> ").append(line.copy().withStyle(ChatFormatting.WHITE)));
    }

    public void setDepartAt(long gameTime) {
        this.departAt = gameTime;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (departAt > 0L && this.level() instanceof ServerLevel level && level.getGameTime() > departAt) {
            this.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public float trueMana() {
        return 200.0F;
    }

    @Override
    public int concealLevel() {
        return 2;
    }

    @Override
    public byte senseKind() {
        return 2;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("DepartAt", departAt);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        departAt = input.getLongOr("DepartAt", 0L);
    }
}
