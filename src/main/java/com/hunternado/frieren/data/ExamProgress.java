package com.hunternado.frieren.data;

import com.hunternado.frieren.magic.MageRank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

/** State of the exam a player is currently sitting. Owned by {@link MagicData}, driven by ExamManager. */
public final class ExamProgress {
    private final MageRank target;
    private int stage;
    private int counter;
    private int secondaryCounter;
    private long deadline;
    private @Nullable BlockPos objective;

    public ExamProgress(MageRank target, long deadline) {
        this.target = target;
        this.deadline = deadline;
    }

    public MageRank target() { return target; }
    public int stage() { return stage; }
    public int counter() { return counter; }
    public int secondaryCounter() { return secondaryCounter; }
    public long deadline() { return deadline; }
    public @Nullable BlockPos objective() { return objective; }

    public void setStage(int stage) {
        this.stage = stage;
        this.counter = 0;
        this.secondaryCounter = 0;
    }

    public void setCounter(int counter) { this.counter = counter; }
    public void incrementCounter() { this.counter++; }
    public void incrementSecondaryCounter() { this.secondaryCounter++; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
    public void setObjective(@Nullable BlockPos objective) { this.objective = objective; }

    public ExamProgress copy() {
        ExamProgress copy = new ExamProgress(target, deadline);
        copy.stage = stage;
        copy.counter = counter;
        copy.secondaryCounter = secondaryCounter;
        copy.objective = objective;
        return copy;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("target", target.serializedName());
        tag.putInt("stage", stage);
        tag.putInt("counter", counter);
        tag.putInt("counter2", secondaryCounter);
        tag.putLong("deadline", deadline);
        if (objective != null) {
            tag.putLong("objective", objective.asLong());
        }
        return tag;
    }

    public static ExamProgress load(CompoundTag tag) {
        ExamProgress progress = new ExamProgress(MageRank.byName(tag.getStringOr("target", "")), tag.getLongOr("deadline", 0L));
        progress.stage = tag.getIntOr("stage", 0);
        progress.counter = tag.getIntOr("counter", 0);
        progress.secondaryCounter = tag.getIntOr("counter2", 0);
        if (tag.contains("objective")) {
            progress.objective = BlockPos.of(tag.getLongOr("objective", 0L));
        }
        return progress;
    }
}
