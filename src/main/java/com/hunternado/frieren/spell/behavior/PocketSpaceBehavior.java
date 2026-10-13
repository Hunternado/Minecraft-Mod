package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;

/**
 * The spell to open a pocket of space: reaches into the caster's ender chest from anywhere. No chest block is
 * involved, so the container stays valid wherever the caster stands until they close it.
 */
public class PocketSpaceBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        PlayerEnderChestContainer pocket = ctx.caster().getEnderChestInventory();
        pocket.setActiveChest(null);
        ctx.caster().openMenu(new SimpleMenuProvider((id, inventory, player) -> ChestMenu.threeRows(id, inventory, pocket),
            Component.translatable("container.frieren.pocket_space")));
        SpellFx.burst(ctx.level(), ctx.caster().getEyePosition().add(ctx.caster().getLookAngle()), ctx.color(), 0.6F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), SoundEvents.ENDER_CHEST_OPEN, 0.6F, 1.2F);
        return true;
    }
}
