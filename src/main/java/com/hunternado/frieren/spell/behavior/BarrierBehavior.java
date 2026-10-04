package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Defensive Magic: a toggled hexagonal barrier. While active, incoming damage is absorbed by spending mana
 * (see {@code CombatEvents}); a small upkeep is drained every second. Params: {@code upkeep},
 * {@code efficiency} (mana per damage point), {@code melee_block} (fraction of melee damage absorbed).
 */
public class BarrierBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        ctx.data().setBarrierSpell(ctx.spell().id());
        SpellFx.hexagon(ctx.level(), ctx.caster().position().add(0, 1.0D, 0), ctx.color(), 1.3F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.BARRIER_UP.get(), 0.8F, 1.2F);
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.barrier_on"));
        return true;
    }

    @Override
    public boolean isToggle() {
        return true;
    }

    @Override
    public boolean isActive(MagicData data) {
        return data.barrierActive();
    }

    @Override
    public void deactivate(ServerPlayer player, MagicData data) {
        data.setBarrierSpell(null);
        Messages.actionBar(player, Component.translatable("message.frieren.barrier_off"));
    }

    @Override
    public void tickActive(ServerPlayer player, MagicData data, SpellDefinition definition) {
        if (player.level().getGameTime() % 20L != 0L) {
            return;
        }
        float upkeep = (float) definition.param("upkeep", 0.6D);
        if (!data.trySpendMana(upkeep)) {
            shatter(player, data);
        }
    }

    /** Called when the barrier runs out of mana. */
    public static void shatter(ServerPlayer player, MagicData data) {
        data.setBarrierSpell(null);
        SpellFx.burst(player.level(), player.position().add(0, 1.0D, 0), 0x9FD8FF, 1.5F);
        SpellFx.sound(player.level(), player.position(), ModSounds.BARRIER_BREAK.get(), 1.0F, 0.9F);
        Messages.actionBar(player, Component.translatable("message.frieren.barrier_broken"));
    }
}
