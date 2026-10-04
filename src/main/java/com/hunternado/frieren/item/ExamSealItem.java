package com.hunternado.frieren.item;

import com.hunternado.frieren.data.ExamProgress;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Issued by examiners: shows the current exam stage, the time left and the way to the objective. */
public class ExamSealItem extends Item {
    public ExamSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        MagicData data = MagicData.get(serverPlayer);
        ExamProgress progress = data != null ? data.exam() : null;
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (progress == null || type == null) {
            Messages.chat(player, Component.translatable("message.frieren.seal_idle"));
            return InteractionResult.SUCCESS;
        }
        long secondsLeft = Math.max(0L, (progress.deadline() - level.getGameTime()) / 20L);
        Messages.chat(player, Component.translatable("message.frieren.seal_status", type.rank().displayName(),
            type.stage(progress.stage()).title(), String.format("%d:%02d", secondsLeft / 60L, secondsLeft % 60L)).withStyle(ChatFormatting.GOLD));
        BlockPos objective = progress.objective();
        if (objective != null) {
            int dx = objective.getX() - player.getBlockX();
            int dz = objective.getZ() - player.getBlockZ();
            Messages.chat(player, Component.translatable("message.frieren.seal_objective", dx, dz, (int) Math.sqrt((double) dx * dx + (double) dz * dz))
                .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.frieren.exam_seal").withStyle(ChatFormatting.GRAY));
    }
}
