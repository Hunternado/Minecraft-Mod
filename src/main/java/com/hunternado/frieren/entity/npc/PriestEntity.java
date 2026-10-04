package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Priest of the Goddess. Heals visitors (once every few minutes per player) and trades holy-magic grimoires.
 * The flask under the counter is purely medicinal.
 */
public class PriestEntity extends FrierenNpcBase {
    private static final long HEAL_COOLDOWN = 6000L;
    private final Map<UUID, Long> lastHealed = new HashMap<>();

    public PriestEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected String npcId() {
        return "priest";
    }

    @Override
    public List<NpcTrades.Offer> offers() {
        return NpcTrades.priest(this.getUUID().getLeastSignificantBits());
    }

    @Override
    public String tradeTitleKey() {
        return "screen.frieren.trades.priest";
    }

    @Override
    public void onInteract(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = lastHealed.getOrDefault(player.getUUID(), -HEAL_COOLDOWN);
        if (now - last >= HEAL_COOLDOWN && player.getHealth() < player.getMaxHealth()) {
            lastHealed.put(player.getUUID(), now);
            player.heal(player.getMaxHealth());
            Messages.chat(player, Component.translatable("npc.frieren.priest.heal"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEAL.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else {
            sayRandomLine(player);
        }
        NpcInteractions.openTrades(player, this);
    }
}
