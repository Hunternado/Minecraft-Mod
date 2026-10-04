package com.hunternado.frieren.entity.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.List;

/** A travelling mage who trades grimoires, staffs and potions. */
public class WanderingMageEntity extends FrierenNpcBase {
    public WanderingMageEntity(EntityType<? extends WanderingMageEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected String npcId() {
        return "wandering_mage";
    }

    @Override
    public List<NpcTrades.Offer> offers() {
        return NpcTrades.wanderingMage(this.getUUID().getLeastSignificantBits());
    }

    @Override
    public String tradeTitleKey() {
        return "screen.frieren.trades.wandering_mage";
    }

    @Override
    public void onInteract(ServerPlayer player) {
        sayRandomLine(player);
        NpcInteractions.openTrades(player, this);
    }
}
