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
 * Passive artifact; active while in the hotbar or offhand (checked once per second by ManaHelper, so no
 * accessory-slot dependency is needed). Duplicates do not stack.
 */
public class ArtifactItem extends Item {
    public enum Kind {
        MIRROR_LOTUS_RING("mirror_lotus_ring"),
        CONCEALMENT_CHARM("concealment_charm"),
        DEMON_HUNTER_INSIGNIA("demon_hunter_insignia");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final Kind kind;

    public ArtifactItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(FrierenIds.key("tooltip", kind.id)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(FrierenIds.key("tooltip", kind.id) + ".effect").withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("tooltip.frieren.artifact_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
