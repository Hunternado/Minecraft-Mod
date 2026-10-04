package com.hunternado.frieren.data;

import com.hunternado.frieren.exam.ExamManager;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.spell.SpellCaster;
import com.hunternado.frieren.spell.behavior.FlightBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Lifecycle of {@link MagicData}: attachment, per-tick upkeep, death/respawn copying and network sync.
 */
public final class MagicDataEvents {
    private static final int MANA_SYNC_INTERVAL = 10;

    private MagicDataEvents() {}

    public static void register() {
        AttachCapabilitiesEvent.Entities.BUS.addListener(MagicDataEvents::onAttach);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(MagicDataEvents::onPlayerTick);
        PlayerEvent.Clone.BUS.addListener(MagicDataEvents::onClone);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(MagicDataEvents::onLogin);
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(MagicDataEvents::onRespawn);
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(MagicDataEvents::onChangeDimension);
        LivingDeathEvent.BUS.addListener(MagicDataEvents::onDeath);
    }

    private static void onAttach(AttachCapabilitiesEvent.Entities event) {
        if (event.getObject() instanceof Player) {
            MagicDataProvider provider = new MagicDataProvider();
            event.addCapability(MagicDataProvider.KEY, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        ManaHelper.serverTick(player, data);
        SpellCaster.serverTick(player, data);
        ExamManager.tick(player, data);

        if (data.fullSyncNeeded()) {
            FrierenNetwork.syncFull(player, data);
        } else if (player.tickCount % MANA_SYNC_INTERVAL == 0
            && (Math.abs(data.mana() - data.lastSyncedMana()) >= 0.5F || data.maxMana() != data.lastSyncedMaxMana())) {
            FrierenNetwork.syncMana(player, data);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        MagicData oldData = MagicData.get(original);
        MagicData newData = MagicData.get(event.getEntity());
        if (oldData != null && newData != null) {
            newData.copyFrom(oldData, event.isWasDeath());
        }
        original.invalidateCaps();
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        // Transient magic never survives a relog; make sure abilities match.
        if (data.flightActive()) {
            FlightBehavior.land(player);
        }
        data.resetTransient();
        ManaHelper.refreshEquipment(player, data);
        data.setCachedMaxMana(ManaHelper.computeMaxMana(data));
        if (!data.starterGiven()) {
            data.setStarterGiven(true);
            ItemStack journal = new ItemStack(ModItems.JOURNEY_JOURNAL.get());
            if (!player.getInventory().add(journal)) {
                player.drop(journal, false);
            }
        }
        FrierenNetwork.syncSpells(player);
        FrierenNetwork.syncFull(player, data);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.markDirty();
            }
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.stopChannel();
                data.clearPendingCast();
                data.markDirty();
            }
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        data.setLastDeath(player.level().dimension().identifier().toString(), player.blockPosition());
        SpellCaster.deactivateAll(player, data);
        ExamManager.onExamineeDeath(player, data);
    }
}
