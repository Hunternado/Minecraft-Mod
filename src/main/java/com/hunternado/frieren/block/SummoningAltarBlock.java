package com.hunternado.frieren.block;

import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.item.SummoningSigilItem;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Summoning Altar found in Aura's fortress, Qual's shrine and the Ruined King's Tomb. Using the matching
 * sigil summons the boss (one per altar at a time), which keeps bosses replayable and multiplayer-friendly.
 */
public class SummoningAltarBlock extends Block {
    public SummoningAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(stack.getItem() instanceof SummoningSigilItem sigil)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        List<Entity> existing = serverLevel.getEntities((Entity) null, new AABB(pos).inflate(48.0D), entity -> entity instanceof FrierenBoss);
        if (!existing.isEmpty()) {
            Messages.actionBar(player, Component.translatable("message.frieren.altar_busy"));
            return InteractionResult.FAIL;
        }
        EntityType<? extends Mob> type = switch (sigil.boss()) {
            case AURA -> ModEntities.AURA.get();
            case QUAL -> ModEntities.QUAL.get();
            case SPIEGEL -> ModEntities.SPIEGEL.get();
        };
        Mob boss = type.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            return InteractionResult.FAIL;
        }
        boss.snapTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 3.5D, 180.0F, 0.0F);
        boss.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
        boss.setPersistenceRequired();
        serverLevel.addFreshEntity(boss);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.playSound(null, pos, ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
        Messages.nearby(serverLevel, boss.position(), 48.0D,
            Component.translatable("message.frieren.summoned." + sigil.boss().id()).withStyle(ChatFormatting.DARK_RED));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            Messages.actionBar(player, Component.translatable("message.frieren.altar_hint"));
        }
        return InteractionResult.SUCCESS;
    }
}
