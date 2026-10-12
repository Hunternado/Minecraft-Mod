package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * The spell to cook a meal: cooks raw food from the caster's inventory, a stack's worth at a time. Params:
 * {@code max_items}.
 */
public class CookBehavior implements SpellBehavior {
    private static final Map<Item, Item> COOKED = Map.ofEntries(
        Map.entry(Items.BEEF, Items.COOKED_BEEF), Map.entry(Items.PORKCHOP, Items.COOKED_PORKCHOP),
        Map.entry(Items.CHICKEN, Items.COOKED_CHICKEN), Map.entry(Items.MUTTON, Items.COOKED_MUTTON),
        Map.entry(Items.RABBIT, Items.COOKED_RABBIT), Map.entry(Items.COD, Items.COOKED_COD),
        Map.entry(Items.SALMON, Items.COOKED_SALMON), Map.entry(Items.POTATO, Items.BAKED_POTATO),
        Map.entry(Items.KELP, Items.DRIED_KELP));

    @Override
    public boolean cast(SpellContext ctx) {
        int budget = (int) ctx.param("max_items", 16.0D);
        int cooked = 0;
        Inventory inventory = ctx.caster().getInventory();
        for (int i = 0; i < inventory.getContainerSize() && budget > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            Item result = COOKED.get(stack.getItem());
            if (result == null || stack.isEmpty()) {
                continue;
            }
            int amount = Math.min(budget, stack.getCount());
            stack.shrink(amount);
            budget -= amount;
            cooked += amount;
            ItemStack meal = new ItemStack(result, amount);
            if (!inventory.add(meal)) {
                ctx.caster().drop(meal, false);
            }
        }
        if (cooked == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.cook_nothing"));
            return false;
        }
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.cook_done", cooked));
        SpellFx.cone(ctx.level(), ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), ctx.color(), 1.5F, 0);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.HELLFIRE.get(), 0.4F, 1.6F);
        return true;
    }
}
