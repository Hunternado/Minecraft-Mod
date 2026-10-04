package com.hunternado.frieren.quest;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.boss.AuraEntity;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.mana.ManaHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/** Kill-based quest hooks. Boss kills credit every player who helped (within 48 blocks). */
public final class QuestEvents {
    private QuestEvents() {}

    public static void register() {
        LivingDeathEvent.BUS.addListener(QuestEvents::onDeath);
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (victim instanceof FrierenBoss && victim.level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(victim) > 48.0D * 48.0D) {
                    continue;
                }
                MagicData data = MagicData.get(player);
                if (data == null) {
                    continue;
                }
                data.incrementStat(QuestStats.BOSSES_SLAIN);
                ManaHelper.grantBonusGrowth(player, data, ((FrierenBoss) victim).bonusGrowthReward());
                if (victim instanceof AuraEntity) {
                    data.incrementStat(QuestStats.AURA_SLAIN);
                    QuestManager.onStat(player, data, QuestStats.AURA_SLAIN);
                }
                if (victim instanceof DemonEntity) {
                    data.incrementStat(QuestStats.DEMONS_SLAIN);
                    QuestManager.onStat(player, data, QuestStats.DEMONS_SLAIN);
                }
            }
            return;
        }
        if (victim instanceof DemonEntity && event.getSource().getEntity() instanceof ServerPlayer killer) {
            MagicData data = MagicData.get(killer);
            if (data != null) {
                data.incrementStat(QuestStats.DEMONS_SLAIN);
                QuestManager.onStat(killer, data, QuestStats.DEMONS_SLAIN);
            }
        }
    }
}
