package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The spell to return home: after a long incantation, carries the caster to where they would respawn (their bed or
 * charged anchor, or the world spawn when there is none), across dimensions if need be. Uses vanilla's own respawn
 * lookup without spending an anchor charge, so an obstructed bed falls back to world spawn exactly as dying would.
 */
public class RecallBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        ServerPlayer caster = ctx.caster();
        Vec3 from = caster.position();
        if (caster.isPassenger()) {
            caster.stopRiding();
        }
        TeleportTransition home = caster.findRespawnPositionAndUseSpawnBlock(false, entity -> {});
        SpellFx.burst(ctx.level(), from.add(0.0D, 1.0D, 0.0D), ctx.color(), 1.2F);
        SpellFx.sound(ctx.level(), from, ModSounds.TELEPORT.get(), 1.0F, 1.0F);
        if (caster.teleport(home) == null) {
            return false;
        }
        caster.resetFallDistance();
        SpellFx.ring(caster.level(), caster.position().add(0.0D, 0.1D, 0.0D), ctx.color(), 1.5F);
        SpellFx.sound(caster.level(), caster.position(), ModSounds.TELEPORT.get(), 1.0F, 1.2F);
        Messages.actionBar(caster, Component.translatable("message.frieren.recall_done"));
        return true;
    }
}
