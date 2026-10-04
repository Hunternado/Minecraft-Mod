package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Serie, founder of the Continental Magic Association, who conducts the First-Class interview. Her mana is
 * perfectly concealed except for a brief flicker every eight seconds — noticing that flicker is the test.
 */
public class SerieEntity extends FrierenNpcBase {
    private static final int FLICKER_PERIOD = 160;
    private static final int FLICKER_LENGTH = 30;

    public SerieEntity(EntityType<? extends SerieEntity> type, Level level) {
        super(type, level);
    }

    /** Spawned for an interview: leaves again after five minutes. */
    public void setTemporary(boolean temporary) {
        if (temporary) {
            setDepartAt(this.level().getGameTime() + 6000L);
        }
    }

    public boolean isFlickering() {
        return this.tickCount % FLICKER_PERIOD < FLICKER_LENGTH;
    }

    public int flickerConcealLevel() {
        return 4;
    }

    @Override
    protected boolean wanders() {
        return false;
    }

    @Override
    protected String npcId() {
        return "serie";
    }

    @Override
    public float trueMana() {
        return 5000.0F;
    }

    @Override
    public int concealLevel() {
        return isFlickering() ? flickerConcealLevel() : 12;
    }

    @Override
    public void onInteract(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        ExamType type = data != null && data.exam() != null ? ExamType.forRank(data.exam().target()) : null;
        if (type != null && type.stage(data.exam().stage()) == ExamType.Stage.INTERVIEW) {
            Messages.chat(player, Component.translatable("npc.frieren.serie.interview").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            sayRandomLine(player);
        }
    }
}
