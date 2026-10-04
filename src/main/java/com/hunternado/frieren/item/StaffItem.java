package com.hunternado.frieren.item;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Mage's staff: a mana focus held in either hand while casting. Casting bare-handed uses {@link Tier#NONE}.
 */
public class StaffItem extends Item {
    public enum Tier {
        NONE("none", 1.40F, 1.00F, 0.85F),
        APPRENTICE("apprentice", 1.00F, 1.00F, 1.00F),
        MAGE("mage", 0.90F, 0.90F, 1.15F),
        SAGE("sage", 0.80F, 0.80F, 1.30F),
        GUILLOTINE("guillotine", 0.75F, 0.75F, 1.50F);

        private final String id;
        private final float costMultiplier;
        private final float cooldownMultiplier;
        private final float powerMultiplier;

        Tier(String id, float costMultiplier, float cooldownMultiplier, float powerMultiplier) {
            this.id = id;
            this.costMultiplier = costMultiplier;
            this.cooldownMultiplier = cooldownMultiplier;
            this.powerMultiplier = powerMultiplier;
        }

        public String id() { return id; }
        public float costMultiplier() { return costMultiplier; }
        public float cooldownMultiplier() { return cooldownMultiplier; }
        public float powerMultiplier() { return powerMultiplier; }
    }

    private final Tier tier;

    public StaffItem(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(FrierenIds.key("tooltip", "staff_" + tier.id)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.frieren.staff_stats",
            Math.round((1.0F - tier.costMultiplier) * 100), Math.round((1.0F - tier.cooldownMultiplier) * 100),
            Math.round((tier.powerMultiplier - 1.0F) * 100)).withStyle(ChatFormatting.BLUE));
    }
}
