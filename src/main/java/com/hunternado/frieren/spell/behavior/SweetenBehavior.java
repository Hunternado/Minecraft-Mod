package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The spell to turn sour grapes sweet. Useless, delightful, and exactly the kind of magic Frieren collects:
 * sweet berries in the caster's inventory become Sweet Grapes. Params: {@code max_items}.
 */
public class SweetenBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        int budget = (int) ctx.param("max_items", 32.0D);
        int changed = 0;
        Inventory inventory = ctx.caster().getInventory();
        for (int i = 0; i < inventory.getContainerSize() && budget > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(Items.SWEET_BERRIES)) {
                continue;
            }
            int amount = Math.min(budget, stack.getCount());
            stack.shrink(amount);
            budget -= amount;
            changed += amount;
            ItemStack sweet = new ItemStack(ModItems.SWEET_GRAPES.get(), amount);
            if (!inventory.add(sweet)) {
                ctx.caster().drop(sweet, false);
            }
        }
        if (changed == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.sweeten_nothing"));
            return false;
        }
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.sweeten_done", changed));
        SpellFx.petals(ctx.level(), ctx.caster().position().add(0.0D, 1.0D, 0.0D), ctx.color(), 1.0F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.UTILITY.get(), 0.8F, 1.8F);
        return true;
    }
}
