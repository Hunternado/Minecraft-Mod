package com.hunternado.frieren.entity.statue;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Hero's statue: Himmel cast in copper, sword raised. It slowly oxidises through four patina stages, and the
 * spell to remove rust from bronze statues polishes it back one stage per cast, which is exactly what Frieren
 * spends her time on in the villages Himmel saved. Immovable and indestructible (creative players can remove it).
 */
public class HeroStatueEntity extends Mob {
    public static final int OXIDIZED = 0;
    public static final int WEATHERED = 1;
    public static final int EXPOSED = 2;
    public static final int POLISHED = 3;
    /** Copper blocks' worth of polishing a stage counts for (the statue quest counts polished copper). */
    public static final int POLISH_VALUE = 8;
    /** Average ticks for the patina to advance one stage (about two in-game days). */
    private static final int OXIDIZE_CHANCE = 48000;
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(HeroStatueEntity.class, EntityDataSerializers.INT);

    public HeroStatueEntity(EntityType<? extends HeroStatueEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, OXIDIZED);
    }

    public int stage() {
        return Math.max(OXIDIZED, Math.min(POLISHED, this.entityData.get(DATA_STAGE)));
    }

    public void setStage(int stage) {
        this.entityData.set(DATA_STAGE, Math.max(OXIDIZED, Math.min(POLISHED, stage)));
    }

    /**
     * Rust Removal hit the statue: polish one stage.
     *
     * @return copper blocks' worth of polishing done (0 if it already gleams)
     */
    public int polish(Player caster) {
        if (!(this.level() instanceof ServerLevel level) || stage() >= POLISHED) {
            return 0;
        }
        setStage(stage() + 1);
        Vec3 chest = this.position().add(0.0D, this.getBbHeight() * 0.6D, 0.0D);
        SpellFx.burst(level, chest, 0xFFC890, 1.6F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.UTILITY.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
        if (stage() == POLISHED) {
            SpellFx.petals(level, this.position().add(0.0D, 0.2D, 0.0D), 0x8FB8FF, 3.5F);
            Messages.chat(caster, Component.translatable("message.frieren.statue_polished").withStyle(ChatFormatting.AQUA));
        }
        return POLISH_VALUE;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide() && stage() > OXIDIZED && this.getRandom().nextInt(OXIDIZE_CHANCE) == 0) {
            setStage(stage() - 1);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide()) {
            Messages.chat(player, Component.translatable("message.frieren.statue_inscription").withStyle(ChatFormatting.GOLD));
            Messages.chat(player, Component.translatable("message.frieren.statue_stage." + stage()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.isCreativePlayer() && source.getEntity() instanceof Player player && player.isShiftKeyDown()) {
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Stage", stage());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setStage(input.getIntOr("Stage", OXIDIZED));
    }
}
