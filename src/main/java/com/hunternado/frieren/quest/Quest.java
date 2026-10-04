package com.hunternado.frieren.quest;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Journey Log entries. Each quest watches one player statistic and completes when it reaches a target.
 * Text lives in the language file under {@code quest.frieren.<id>} / {@code .desc} / {@code .reward}.
 */
public enum Quest {
    JOURNEY_BEGINS("journey_begins", QuestStats.SPELLS_LEARNED, 1,
        (player, data) -> give(player, new ItemStack(ModItems.APPRENTICE_STAFF.get()))),
    A_FIELD_OF_FLOWERS("a_field_of_flowers", QuestStats.FLOWER_FIELDS, 10,
        (player, data) -> give(player, new ItemStack(ModItems.BLUE_MOON_WEED.get(), 4))),
    THE_HEROS_STATUE("the_heros_statue", QuestStats.COPPER_POLISHED, 24,
        (player, data) -> give(player, new ItemStack(ModItems.MIRROR_LOTUS_RING.get()))),
    MIMIC_HUNTER("mimic_hunter", QuestStats.MIMIC_RESEARCH, 3,
        (player, data) -> give(player, GrimoireItem.unidentified(SpellRarity.RARE))),
    SPELL_COLLECTOR("spell_collector", QuestStats.SPELLS_LEARNED, 10,
        (player, data) -> ManaHelper.grantBonusGrowth(player, data, 25)),
    DEMON_SLAYER("demon_slayer", QuestStats.DEMONS_SLAIN, 25,
        (player, data) -> give(player, new ItemStack(ModItems.DEMON_HUNTER_INSIGNIA.get()))),
    AURA_KILL_YOURSELF("aura_kill_yourself", QuestStats.AURA_SLAIN, 1,
        (player, data) -> {
            ManaHelper.grantBonusGrowth(player, data, 60);
            give(player, GrimoireItem.unidentified(SpellRarity.LEGENDARY));
        });

    private final String id;
    private final String stat;
    private final int target;
    private final BiConsumer<ServerPlayer, MagicData> reward;

    Quest(String id, String stat, int target, BiConsumer<ServerPlayer, MagicData> reward) {
        this.id = id;
        this.stat = stat;
        this.target = target;
        this.reward = reward;
    }

    public String id() { return id; }
    public String stat() { return stat; }
    public int target() { return target; }

    public void grantReward(ServerPlayer player, MagicData data) {
        reward.accept(player, data);
    }

    public Component title() {
        return Component.translatable(FrierenIds.key("quest", id));
    }

    public Component description() {
        return Component.translatable(FrierenIds.key("quest", id) + ".desc");
    }

    public Component rewardText() {
        return Component.translatable(FrierenIds.key("quest", id) + ".reward");
    }

    public static @Nullable Quest byId(String id) {
        for (Quest quest : values()) {
            if (quest.id.equals(id)) {
                return quest;
            }
        }
        return null;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
