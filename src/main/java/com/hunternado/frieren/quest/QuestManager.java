package com.hunternado.frieren.quest;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Advances Journey Log quests from gameplay hooks. All methods are server-side. */
public final class QuestManager {
    private static final net.minecraft.resources.Identifier FLOWER_FIELD_BEHAVIOR = FrierenIds.id("flower_field");

    private QuestManager() {}

    public static void onSpellCast(ServerPlayer player, MagicData data, SpellDefinition spell, int hits) {
        if (spell.behavior().equals(FLOWER_FIELD_BEHAVIOR)) {
            data.incrementStat(QuestStats.FLOWER_FIELDS);
            onStat(player, data, QuestStats.FLOWER_FIELDS);
        }
    }

    public static void onSpellLearned(ServerPlayer player, MagicData data) {
        data.setStat(QuestStats.SPELLS_LEARNED, data.knownCount());
        onStat(player, data, QuestStats.SPELLS_LEARNED);
        if (data.knownCount() == 1) {
            ModTriggers.milestone(player, "first_spell");
        }
    }

    /** Re-evaluates every quest watching {@code stat}. */
    public static void onStat(ServerPlayer player, MagicData data, String stat) {
        int value = data.stat(stat);
        for (Quest quest : Quest.values()) {
            if (quest.stat().equals(stat) && !data.questCompleted(quest.id()) && value >= quest.target()) {
                complete(player, data, quest);
            }
        }
    }

    public static void complete(ServerPlayer player, MagicData data, Quest quest) {
        data.completeQuest(quest.id());
        quest.grantReward(player, data);
        player.sendSystemMessage(Component.translatable("message.frieren.quest_complete", quest.title().copy().withStyle(ChatFormatting.GOLD))
            .append(Component.literal(" — ").withStyle(ChatFormatting.GRAY))
            .append(quest.rewardText().copy().withStyle(ChatFormatting.GRAY)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RANK_UP.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
        ModTriggers.milestone(player, "quest_" + quest.id());
    }

    /** Recomputes derived stats (used after admin commands). */
    public static void refreshAll(ServerPlayer player, MagicData data) {
        data.setStat(QuestStats.SPELLS_LEARNED, data.knownCount());
        for (Quest quest : Quest.values()) {
            onStat(player, data, quest.stat());
        }
    }
}
