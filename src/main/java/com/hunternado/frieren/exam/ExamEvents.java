package com.hunternado.frieren.exam;

import com.hunternado.frieren.data.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.function.Predicate;

/** Wires exam trials into the game: deaths, ticking, login/logout and refusing stale trial entities. */
public final class ExamEvents {
    private ExamEvents() {}

    public static void register() {
        LivingDeathEvent.BUS.addListener(ExamEvents::onDeath);
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> TrialManager.tick(event.server()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ExamEvents::onLogin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(ExamEvents::onLogout);
        EntityJoinLevelEvent.BUS.addListener((Predicate<EntityJoinLevelEvent>) ExamEvents::onJoin);
        ServerStoppingEvent.BUS.addListener(event -> TrialManager.endAll(event.getServer()));
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            TrialManager.onDeath(event.getEntity());
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                ExamManager.onLogin(player, data);
            }
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                ExamManager.onLogout(player, data);
            }
        }
    }

    /** @return true to refuse the entity: a trial entity loaded from disk that no running trial owns. */
    private static boolean onJoin(EntityJoinLevelEvent event) {
        return event.loadedFromDisk() && !event.getLevel().isClientSide() && TrialManager.shouldRefuseLoaded(event.getEntity());
    }
}
