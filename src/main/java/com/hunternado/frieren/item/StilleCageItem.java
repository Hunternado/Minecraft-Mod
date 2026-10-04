package com.hunternado.frieren.item;

import com.hunternado.frieren.entity.creature.StilleEntity;
import com.hunternado.frieren.registry.ModDataComponents;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** First-Class exam stage one: use on an exhausted Stille to capture it, then bring it to the examiner. */
public class StilleCageItem extends Item {
    public StilleCageItem(Properties properties) {
        super(properties);
    }

    public static boolean isFull(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.CAGED_STILLE.get()));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof StilleEntity stille) || isFull(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!stille.isExhausted()) {
            Messages.actionBar(player, Component.translatable("message.frieren.stille_too_fast"));
            return InteractionResult.FAIL;
        }
        if (!stille.mayBeCapturedBy(player)) {
            Messages.actionBar(player, Component.translatable("message.frieren.stille_not_yours"));
            return InteractionResult.FAIL;
        }
        ItemStack held = player.getItemInHand(hand);
        held.set(ModDataComponents.CAGED_STILLE.get(), true);
        stille.discard();
        Messages.actionBar(player, Component.translatable("message.frieren.stille_caught").withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isFull(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(isFull(stack) ? "tooltip.frieren.stille_cage_full" : "tooltip.frieren.stille_cage")
            .withStyle(ChatFormatting.GRAY));
    }
}
