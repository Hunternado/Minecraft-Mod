package com.hunternado.frieren.spell;

import com.hunternado.frieren.data.MagicData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Code-backed effect of a spell. Implementations are stateless singletons registered in
 * {@link SpellBehaviors}; all per-cast data comes from the {@link SpellContext} and the spell's JSON params.
 * Every method runs on the logical server.
 */
public interface SpellBehavior {
    /**
     * Executes the spell. Mana has already been paid.
     *
     * @return false if nothing happened (e.g. no valid target); the caster is refunded and no cooldown applies.
     */
    boolean cast(SpellContext ctx);

    /**
     * Channelled spells only: called every tick while the cast key is held, starting the tick after
     * {@link #cast}. Return false to end the channel.
     */
    default boolean tickChannel(SpellContext ctx, int channelTick) {
        return false;
    }

    /** Toggle spells (flight, defensive barrier) are switched off for free by casting them again. */
    default boolean isToggle() {
        return false;
    }

    default boolean isActive(MagicData data) {
        return false;
    }

    default void deactivate(ServerPlayer player, MagicData data) {}

    /** Per-tick upkeep while a toggle is active. */
    default void tickActive(ServerPlayer player, MagicData data, SpellDefinition definition) {}
}
