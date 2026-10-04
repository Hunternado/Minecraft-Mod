package com.hunternado.frieren.item;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Consumed by a Summoning Altar to call a named demon or Spiegel. */
public class SummoningSigilItem extends Item {
    public enum Boss {
        AURA("aura"),
        QUAL("qual"),
        SPIEGEL("spiegel");

        private final String id;

        Boss(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final Boss boss;

    public SummoningSigilItem(Properties properties, Boss boss) {
        super(properties);
        this.boss = boss;
    }

    public Boss boss() {
        return boss;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(FrierenIds.key("tooltip", "sigil_" + boss.id)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.frieren.sigil_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
