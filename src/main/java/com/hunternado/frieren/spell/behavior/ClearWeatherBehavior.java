package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The spell to clear the skies: ends rain and thunder in the Overworld for a while. Params: {@code clear_ticks}.
 */
public class ClearWeatherBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        if (ctx.level().dimension() != Level.OVERWORLD) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.weather_no_sky"));
            return false;
        }
        int clear = (int) ctx.param("clear_ticks", 12000.0D);
        ctx.level().getServer().setWeatherParameters(clear, 0, false, false);
        SpellFx.burst(ctx.level(), ctx.caster().position().add(0.0D, 2.0D, 0.0D), ctx.color(), 2.0F);
        SpellFx.beam(ctx.level(), ctx.caster().position().add(0.0D, 1.5D, 0.0D), ctx.caster().position().add(0.0D, 40.0D, 0.0D), ctx.color());
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.WIND.get(), 1.0F, 1.1F);
        Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.weather_cleared"));
        return true;
    }
}
