package com.hunternado.frieren.item;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.data.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Restores mana when drunk. Heiter's Flask restores far more but leaves you as unsteady as the old priest
 * always was, and is refilled rather than consumed.
 */
public class ManaPotionItem extends Item {
    private final float restore;
    private final boolean heiter;

    public ManaPotionItem(Properties properties, float restore, boolean heiter) {
        super(properties);
        this.restore = restore;
        this.heiter = heiter;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return heiter ? 40 : 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.setMana(data.mana() + restore);
            }
            if (heiter) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                player.getCooldowns().addCooldown(stack, 1200);
                return stack;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    return bottle;
                }
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.frieren.restores_mana", (int) restore).withStyle(ChatFormatting.AQUA));
        if (heiter) {
            tooltip.accept(Component.translatable(FrierenIds.key("tooltip", "heiter_flask")).withStyle(ChatFormatting.GRAY));
        }
    }
}
