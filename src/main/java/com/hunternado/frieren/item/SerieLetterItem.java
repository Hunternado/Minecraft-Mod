package com.hunternado.frieren.item;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A letter from Serie, sent to every new First-Class mage. Reading it calls her to you for ten minutes, at most once
 * a day; it is never used up. With Serie there you can show her your magic (her appraisal) and, once she is
 * impressed and you have beaten Übel, ask her to create a spell for you.
 */
public class SerieLetterItem extends TooltipItem {
    public static final long COOLDOWN = 24000L;

    public SerieLetterItem(Properties properties) {
        super(properties, "serie_letter");
    }

    /** First-Class mages get the letter once: on passing the exam, or on their first login after this update. */
    public static void grantOnce(ServerPlayer player, MagicData data) {
        if (data.rank() != MageRank.FIRST_CLASS || data.stat("serie_letter") > 0) {
            return;
        }
        data.setStat("serie_letter", 1);
        ItemStack letter = new ItemStack(ModItems.SERIE_LETTER.get());
        if (!player.getInventory().add(letter)) {
            player.drop(letter, false);
        }
        Messages.chat(player, Component.translatable("message.frieren.serie_letter_received").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        MagicData data = MagicData.get(serverPlayer);
        if (data == null) {
            return InteractionResult.FAIL;
        }
        if (data.rank() != MageRank.FIRST_CLASS) {
            Messages.actionBar(player, Component.translatable("message.frieren.serie_letter_rank"));
            return InteractionResult.FAIL;
        }
        if (!serverLevel.getEntitiesOfClass(SerieEntity.class, new AABB(player.blockPosition()).inflate(48.0D)).isEmpty()) {
            Messages.actionBar(player, Component.translatable("message.frieren.serie_letter_here"));
            return InteractionResult.FAIL;
        }
        long now = serverLevel.getGameTime();
        if (now < data.serieLetterReadyAt() && !player.getAbilities().instabuild) {
            long minutes = Math.max(1L, (data.serieLetterReadyAt() - now) / 1200L);
            Messages.actionBar(player, Component.translatable("message.frieren.serie_letter_wait", minutes));
            return InteractionResult.FAIL;
        }
        SerieEntity serie = ModEntities.SERIE.get().create(serverLevel, EntitySpawnReason.EVENT);
        if (serie == null) {
            return InteractionResult.FAIL;
        }
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 at = player.position().add(look.scale(3.0D));
        serie.snapTo(at.x, player.getY(), at.z, player.getYRot() + 180.0F, 0.0F);
        serie.setVisit(12000L);
        serverLevel.addFreshEntity(serie);
        data.setSerieLetterReadyAt(now + COOLDOWN);
        SpellFx.burst(serverLevel, serie.position().add(0.0D, 1.0D, 0.0D), 0xF6CB58, 1.5F);
        SpellFx.petals(serverLevel, serie.position(), 0xF6CB58, 1.5F);
        serverLevel.playSound(null, serie.getX(), serie.getY(), serie.getZ(), ModSounds.TELEPORT.get(), SoundSource.NEUTRAL, 1.0F, 1.3F);
        Messages.chat(serverPlayer, Component.literal("<").append(serie.getDisplayName()).append("> ")
            .append(Component.translatable("npc.frieren.serie.summoned").withStyle(ChatFormatting.WHITE)));
        return InteractionResult.SUCCESS;
    }
}
