package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The spell to clean clothes (Fern's choice of Serie's privilege): repairs worn armour and the held items
 * by {@code repair} points each. Fails (refunded) if nothing needs mending.
 */
public class MendBehavior implements SpellBehavior {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    @Override
    public boolean cast(SpellContext ctx) {
        int repair = Math.max(1, Math.round(ctx.scaled("repair", 25.0D)));
        int mended = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = ctx.caster().getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
                mended++;
            }
        }
        if (mended == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.mend_nothing"));
            return false;
        }
        SpellFx.burst(ctx.level(), ctx.caster().position().add(0, 1.0D, 0), ctx.color(), 0.9F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.UTILITY.get(), 0.8F, 1.1F);
        return true;
    }
}
