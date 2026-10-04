package com.hunternado.frieren.event;

import com.hunternado.frieren.combat.ObedienceTracker;
import com.hunternado.frieren.command.FrierenCommand;
import com.hunternado.frieren.entity.npc.FrierenEntity;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Server lifecycle wiring: data reload, client sync, commands and the rare traveller encounter. */
public final class ServerEvents {
    /** Roughly one attempt per in-game day per overworld. */
    private static final int TRAVELLER_INTERVAL = 24000;

    private ServerEvents() {}

    public static void register() {
        AddReloadListenerEvent.BUS.addListener(ServerEvents::onAddReloadListeners);
        OnDatapackSyncEvent.BUS.addListener(ServerEvents::onDatapackSync);
        RegisterCommandsEvent.BUS.addListener(ServerEvents::onRegisterCommands);
        TickEvent.LevelTickEvent.Post.BUS.addListener(ServerEvents::onLevelTick);
        ServerStoppedEvent.BUS.addListener(ServerEvents::onServerStopped);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SpellManager(event.getRegistries()));
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        for (ServerPlayer player : event.getPlayers()) {
            FrierenNetwork.syncSpells(player);
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        FrierenCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (level.getGameTime() % TRAVELLER_INTERVAL == 12000L) {
            FrierenEntity.trySpawnTravellingParty(level);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ObedienceTracker.clear();
    }
}
