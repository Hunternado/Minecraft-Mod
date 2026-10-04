package com.hunternado.frieren.exam;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.magic.SpellSchool;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Continental Magic Association examinations. Requirements gate sitting the exam; stages are objectives
 * tracked by {@link ExamManager}.
 */
public enum ExamType {
    FIFTH_CLASS(MageRank.FIFTH_CLASS, 2, 60, 0, 0, false, new Stage[] {Stage.PRACTICAL}),
    FOURTH_CLASS(MageRank.FOURTH_CLASS, 4, 0, 25, 1, false, new Stage[] {Stage.SURVIVAL}),
    THIRD_CLASS(MageRank.THIRD_CLASS, 6, 0, 0, 0, true, new Stage[] {Stage.RETRIEVAL}),
    SECOND_CLASS(MageRank.SECOND_CLASS, 9, 0, 50, 2, false, new Stage[] {Stage.LABYRINTH}),
    FIRST_CLASS(MageRank.FIRST_CLASS, 12, 250, 0, 0, false, new Stage[] {Stage.STILLE, Stage.LABYRINTH, Stage.INTERVIEW});

    /** A single objective. Time limits are in ticks before the config multiplier. */
    public enum Stage {
        PRACTICAL("practical", 12000, 5),
        SURVIVAL("survival", 18000, 8),
        RETRIEVAL("retrieval", 48000, 1),
        LABYRINTH("labyrinth", 36000, 1),
        STILLE("stille", 12000, 1),
        INTERVIEW("interview", 6000, 1);

        private final String id;
        private final int timeLimit;
        private final int goal;

        Stage(String id, int timeLimit, int goal) {
            this.id = id;
            this.timeLimit = timeLimit;
            this.goal = goal;
        }

        public String id() { return id; }
        public int timeLimit() { return timeLimit; }
        public int goal() { return goal; }

        public Component title() {
            return Component.translatable(FrierenIds.key("exam.stage", id));
        }

        public Component instructions() {
            return Component.translatable(FrierenIds.key("exam.stage", id) + ".desc");
        }
    }

    public static final int SURVIVAL_DEMONS = 2;

    private final MageRank rank;
    private final int minSpells;
    private final int minMaxMana;
    private final int masteryLevel;
    private final int masteryCount;
    private final boolean needsDefensive;
    private final Stage[] stages;

    ExamType(MageRank rank, int minSpells, int minMaxMana, int masteryLevel, int masteryCount, boolean needsDefensive, Stage[] stages) {
        this.rank = rank;
        this.minSpells = minSpells;
        this.minMaxMana = minMaxMana;
        this.masteryLevel = masteryLevel;
        this.masteryCount = masteryCount;
        this.needsDefensive = needsDefensive;
        this.stages = stages;
    }

    public MageRank rank() { return rank; }
    public Stage[] stages() { return stages; }
    public Stage stage(int index) { return stages[Math.max(0, Math.min(stages.length - 1, index))]; }

    public static @Nullable ExamType forRank(MageRank rank) {
        for (ExamType type : values()) {
            if (type.rank == rank) {
                return type;
            }
        }
        return null;
    }

    /** Human-readable list of unmet requirements; empty when the player may sit the exam. */
    public List<Component> unmetRequirements(MagicData data, boolean serverSide) {
        List<Component> unmet = new ArrayList<>();
        MageRank previous = MageRank.values()[Math.max(0, rank.ordinal() - 1)];
        if (data.rank() != previous) {
            unmet.add(Component.translatable("exam.frieren.req.rank", previous.displayName()));
        }
        if (data.knownCount() < minSpells) {
            unmet.add(Component.translatable("exam.frieren.req.spells", minSpells, data.knownCount()));
        }
        if (minMaxMana > 0 && data.maxMana() < minMaxMana) {
            unmet.add(Component.translatable("exam.frieren.req.mana", minMaxMana, data.maxMana()));
        }
        if (masteryCount > 0 && data.masteryCount(masteryLevel) < masteryCount) {
            unmet.add(Component.translatable("exam.frieren.req.mastery", masteryCount, masteryLevel, data.masteryCount(masteryLevel)));
        }
        if (needsDefensive && !knowsDefensive(data, serverSide)) {
            unmet.add(Component.translatable("exam.frieren.req.defensive"));
        }
        return unmet;
    }

    private static boolean knowsDefensive(MagicData data, boolean serverSide) {
        for (Identifier id : data.knownSpells().keySet()) {
            SpellDefinition definition = serverSide ? SpellManager.server(id) : SpellManager.client(id);
            if (definition != null && definition.school() == SpellSchool.DEFENSIVE) {
                return true;
            }
        }
        return false;
    }
}
