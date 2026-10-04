package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Flight Magic: toggled survival flight that drains {@code drain_per_second} mana while actually flying.
 * Running dry ends the spell with a few seconds of slow falling so it never kills the caster outright.
 */
public class FlightBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        if (!FrierenConfig.flightMagicEnabled()) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.flight_disabled"));
            return false;
        }
        ServerPlayer player = ctx.caster();
        ctx.data().setFlightSpell(ctx.spell().id());
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        SpellFx.spiral(ctx.level(), player.position(), ctx.color(), 2.0F);
        SpellFx.sound(ctx.level(), player.position(), ModSounds.FLIGHT.get(), 0.8F, 1.2F);
        Messages.actionBar(player, Component.translatable("message.frieren.flight_on"));
        return true;
    }

    @Override
    public boolean isToggle() {
        return true;
    }

    @Override
    public boolean isActive(MagicData data) {
        return data.flightActive();
    }

    @Override
    public void deactivate(ServerPlayer player, MagicData data) {
        data.setFlightSpell(null);
        land(player);
        Messages.actionBar(player, Component.translatable("message.frieren.flight_off"));
    }

    @Override
    public void tickActive(ServerPlayer player, MagicData data, SpellDefinition definition) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (!player.getAbilities().mayfly) {
            // Something else (another mod, gamemode switch) revoked flight: re-grant while the spell is active.
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        if (player.getAbilities().flying) {
            float drain = (float) (definition.param("drain_per_second", 2.0D) / 20.0D);
            if (!data.trySpendMana(drain)) {
                data.setFlightSpell(null);
                land(player);
                Messages.actionBar(player, Component.translatable("message.frieren.flight_exhausted"));
                return;
            }
            data.pauseRegenUntil(player.level().getGameTime() + 20L);
        }
    }

    /** Removes flight safely; also used by death/dimension cleanup. */
    public static void land(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean wasFlying = player.getAbilities().flying;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        if (wasFlying || !player.onGround()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
        }
    }
}
