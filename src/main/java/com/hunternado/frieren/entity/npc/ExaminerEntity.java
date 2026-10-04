package com.hunternado.frieren.entity.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Continental Magic Association examiner: administers rank exams and Serie's privilege. */
public class ExaminerEntity extends FrierenNpcBase {
    public ExaminerEntity(EntityType<? extends ExaminerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean wanders() {
        return false;
    }

    @Override
    protected String npcId() {
        return "examiner";
    }

    @Override
    public float trueMana() {
        return 450.0F;
    }

    @Override
    public void onInteract(ServerPlayer player) {
        sayRandomLine(player);
        NpcInteractions.openExaminer(player, this);
    }
}
