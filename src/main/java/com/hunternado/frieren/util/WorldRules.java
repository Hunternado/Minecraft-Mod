package com.hunternado.frieren.util;

import com.hunternado.frieren.config.FrierenConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

/** Game-rule aware checks for destructive or PvP magic. */
public final class WorldRules {
    private WorldRules() {}

    public static boolean pvp(ServerLevel level) {
        return level.getGameRules().get(GameRules.PVP);
    }

    /** Spells may only change blocks when both the server config and the mobGriefing rule allow it. */
    public static boolean spellGriefing(ServerLevel level) {
        return FrierenConfig.spellGriefing() && level.getGameRules().get(GameRules.MOB_GRIEFING);
    }
}
