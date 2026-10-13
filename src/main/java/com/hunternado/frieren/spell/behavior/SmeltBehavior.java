package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The spell to smelt ore without a furnace: raw metals, sand, cobblestone, clay and logs in the caster's inventory are
 * fired as a furnace would, up to a budget per cast. Food is left to the cooking spell. Results are computed for the
 * whole inventory before anything is added back, so cobblestone becomes stone, never smooth stone. Params:
 * {@code max_items}.
 */
public class SmeltBehavior implements SpellBehavior {
    private static final Map<Item, Item> SMELTED = Map.ofEntries(
        Map.entry(Items.RAW_IRON, Items.IRON_INGOT), Map.entry(Items.RAW_GOLD, Items.GOLD_INGOT),
        Map.entry(Items.RAW_COPPER, Items.COPPER_INGOT), Map.entry(Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP),
        Map.entry(Items.SAND, Items.GLASS), Map.entry(Items.RED_SAND, Items.GLASS),
        Map.entry(Items.COBBLESTONE, Items.STONE), Map.entry(Items.COBBLED_DEEPSLATE, Items.DEEPSLATE),
        Map.entry(Items.STONE_BRICKS, Items.CRACKED_STONE_BRICKS), Map.entry(Items.SANDSTONE, Items.SMOOTH_SANDSTONE),
        Map.entry(Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ), Map.entry(Items.BASALT, Items.SMOOTH_BASALT),
        Map.entry(Items.CLAY_BALL, Items.BRICK), Map.entry(Items.CLAY, Items.TERRACOTTA),
        Map.entry(Items.NETHERRACK, Items.NETHER_BRICK), Map.entry(Items.WET_SPONGE, Items.SPONGE),
        Map.entry(Items.CHORUS_FRUIT, Items.POPPED_CHORUS_FRUIT));

    @Override
    public boolean cast(SpellContext ctx) {
        int budget = (int) ctx.param("max_items", 32.0D);
        Inventory inventory = ctx.caster().getInventory();
        List<ItemStack> results = new ArrayList<>();
        int smelted = 0;
        for (int i = 0; i < inventory.getContainerSize() && budget > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item result = SMELTED.get(stack.getItem());
            if (result == null && stack.is(ItemTags.LOGS_THAT_BURN)) {
                result = Items.CHARCOAL;
            }
            if (result == null) {
                continue;
            }
            int amount = Math.min(budget, stack.getCount());
            stack.shrink(amount);
            budget -= amount;
            smelted += amount;
            results.add(new ItemStack(result, amount));
        }
        if (smelted == 0) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.smelt_nothing"));
            return false;
        }
        for (ItemStack result : results) {
            if (!inventory.add(result)) {
                ctx.caster().drop(result, false);
            }
        }
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.smelt_done", smelted));
        SpellFx.cone(ctx.level(), ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), ctx.color(), 1.5F, 0);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.HELLFIRE.get(), 0.5F, 1.2F);
        return true;
    }
}
