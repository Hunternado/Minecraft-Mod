package com.hunternado.frieren.network;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.client.ClientPacketHandlers;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.network.packet.BindSpellPacket;
import com.hunternado.frieren.network.packet.CastSpellPacket;
import com.hunternado.frieren.network.packet.DetectManaPacket;
import com.hunternado.frieren.network.packet.ExaminerScreenPacket;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import com.hunternado.frieren.network.packet.NpcActionPacket;
import com.hunternado.frieren.network.packet.ReleaseChannelPacket;
import com.hunternado.frieren.network.packet.SelectSlotPacket;
import com.hunternado.frieren.network.packet.SpellFxPacket;
import com.hunternado.frieren.network.packet.SyncMagicDataPacket;
import com.hunternado.frieren.network.packet.SyncManaPacket;
import com.hunternado.frieren.network.packet.SyncSpellsPacket;
import com.hunternado.frieren.network.packet.ToggleConcealmentPacket;
import com.hunternado.frieren.network.packet.TradeScreenPacket;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.List;

/**
 * The mod's single network channel.
 * <p>
 * Clientbound handlers delegate to {@link ClientPacketHandlers} from inside lambda bodies; those bodies only
 * ever execute on the physical client, so the client class is never loaded on a dedicated server.
 */
public final class FrierenNetwork {
    public static final Identifier NAME = FrierenIds.id("main");
    public static final int PROTOCOL_VERSION = 1;

    private static SimpleChannel channel;

    private FrierenNetwork() {}

    public static synchronized void register() {
        if (channel != null) {
            return;
        }
        channel = ChannelBuilder.named(NAME)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel()
                .play()
                    .serverbound()
                        .addMain(CastSpellPacket.class, CastSpellPacket.STREAM_CODEC, CastSpellPacket::handle)
                        .addMain(ReleaseChannelPacket.class, ReleaseChannelPacket.STREAM_CODEC, ReleaseChannelPacket::handle)
                        .addMain(SelectSlotPacket.class, SelectSlotPacket.STREAM_CODEC, SelectSlotPacket::handle)
                        .addMain(BindSpellPacket.class, BindSpellPacket.STREAM_CODEC, BindSpellPacket::handle)
                        .addMain(ToggleConcealmentPacket.class, ToggleConcealmentPacket.STREAM_CODEC, ToggleConcealmentPacket::handle)
                        .addMain(DetectManaPacket.class, DetectManaPacket.STREAM_CODEC, DetectManaPacket::handle)
                        .addMain(NpcActionPacket.class, NpcActionPacket.STREAM_CODEC, NpcActionPacket::handle)
                    .clientbound()
                        .addMain(SyncMagicDataPacket.class, SyncMagicDataPacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.syncMagicData(msg))
                        .addMain(SyncManaPacket.class, SyncManaPacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.syncMana(msg))
                        .addMain(SyncSpellsPacket.class, SyncSpellsPacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.syncSpells(msg))
                        .addMain(ManaSensePacket.class, ManaSensePacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.manaSense(msg))
                        .addMain(SpellFxPacket.class, SpellFxPacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.spellFx(msg))
                        .addMain(ExaminerScreenPacket.class, ExaminerScreenPacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.openExaminer(msg))
                        .addMain(TradeScreenPacket.class, TradeScreenPacket.STREAM_CODEC, (msg, ctx) -> ClientPacketHandlers.openTrades(msg))
            .build();
    }

    // ---- Client → server ----------------------------------------------------------------------

    public static void sendToServer(Object message) {
        channel.send(message, PacketDistributor.SERVER.noArg());
    }

    // ---- Server → client ----------------------------------------------------------------------

    public static void sendToPlayer(ServerPlayer player, Object message) {
        channel.send(message, PacketDistributor.PLAYER.with(player));
    }

    public static void sendNear(ServerLevel level, Vec3 pos, double radius, Object message) {
        channel.send(message, PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, radius, level.dimension())));
    }

    public static void syncFull(ServerPlayer player, MagicData data) {
        sendToPlayer(player, new SyncMagicDataPacket(data.save(), player.level().getGameTime()));
        data.clearFullSync();
        data.markManaSynced();
    }

    public static void syncMana(ServerPlayer player, MagicData data) {
        sendToPlayer(player, new SyncManaPacket(data.mana(), data.maxMana()));
        data.markManaSynced();
    }

    public static void syncSpells(ServerPlayer player) {
        sendToPlayer(player, new SyncSpellsPacket(List.copyOf(SpellManager.serverDefinitions())));
    }
}
