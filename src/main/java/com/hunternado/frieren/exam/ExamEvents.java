package com.hunternado.frieren.exam;

import com.hunternado.frieren.data.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/** Feeds kills into the exam state machine. */
public final class ExamEvents {
    private ExamEvents() {}

    public static void register() {
        LivingDeathEvent.BUS.addListener(ExamEvents::onDeath);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data != null && data.exam() != null) {
            ExamManager.onKill(player, data, event.getEntity(), event.getSource());
        }
    }
}
