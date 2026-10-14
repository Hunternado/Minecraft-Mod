package com.hunternado.frieren.spell;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.CastStatePacket;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * A mage's concentration. Incantations and charges need an unbroken image of the spell; a solid hit can shatter it.
 * <ul>
 *   <li>Hits under 1.5 damage never break it. Above that, the chance grows with the hit (a 6-damage blow is a sure
 *   break for a novice).</li>
 *   <li>Composure cuts the chance: mastery of the spell (up to 45%), defensive magic already up (25%: the barrier
 *   takes the blow) and rank (5% from Second Class, 10% at First Class).</li>
 *   <li>Spells with param {@code steady} never break; a spell's {@code fragility} param multiplies the chance
 *   (Annihilation is a fragile thing to hold).</li>
 * </ul>
 * A broken charge loses all the mana already poured into it; a broken incantation costs a quarter of the spell.
 */
public final class Concentration {
    private static final float MIN_DAMAGE = 1.5F;

    private Concentration() {}

    /** Called from the hurt event for every player hit (after defensive magic had its share). */
    public static void onHurt(ServerPlayer player, MagicData data, DamageSource source, float amount) {
        if (amount < MIN_DAMAGE || source.getEntity() == player) {
            return;
        }
        Identifier spellId = data.chargingSpell() != null ? data.chargingSpell() : data.pendingCast();
        if (spellId == null) {
            return;
        }
        SpellDefinition spell = SpellManager.server(spellId);
        if (spell == null || spell.isSteady()) {
            return;
        }
        if (player.getRandom().nextFloat() >= breakChance(data, spell, amount)) {
            return;
        }
        shatter(player, data, spell);
    }

    public static float breakChance(MagicData data, SpellDefinition spell, float amount) {
        SpellProgress progress = data.progress(spell.id());
        float mastery = progress != null ? progress.masteryFraction() : 0.0F;
        float composure = 0.45F * mastery + (data.barrierActive() ? 0.25F : 0.0F)
            + (data.rank() == MageRank.FIRST_CLASS ? 0.10F : data.rank().atLeast(MageRank.SECOND_CLASS) ? 0.05F : 0.0F);
        float severity = Math.min(1.0F, Math.max(0.25F, amount / 6.0F));
        float chance = severity * (1.0F - Math.min(0.8F, composure)) * (float) spell.param("fragility", 1.0D);
        return Math.min(1.0F, Math.max(0.0F, chance));
    }

    /** Breaks whatever the player is incanting or charging, with the losses described above. */
    public static void shatter(ServerPlayer player, MagicData data, SpellDefinition spell) {
        ServerLevel level = player.level();
        if (data.chargingSpell() != null) {
            data.clearCharge();                                  // the poured mana is simply gone
        } else {
            data.clearPendingCast();
            data.trySpendMana(Math.min(data.mana(), spell.manaCost() * 0.25F));
        }
        data.markDirty();
        SpellFx.burst(level, player.getEyePosition().add(player.getLookAngle().scale(1.2D)), spell.color(), 1.0F);
        SpellFx.hexagon(level, player.position().add(0.0D, 1.0D, 0.0D), spell.color(), 0.9F);
        SpellFx.sound(level, player.position(), ModSounds.BARRIER_BREAK.get(), 0.9F, 1.5F);
        Messages.actionBar(player, Component.translatable("message.frieren.concentration_broken", spell.displayName())
            .withStyle(ChatFormatting.RED));
        FrierenNetwork.sendCastState(player, CastStatePacket.BROKEN, spell.id(), 10);
    }
}
