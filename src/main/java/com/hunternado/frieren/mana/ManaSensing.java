package com.hunternado.frieren.mana;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.exam.ExamManager;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Mana concealment toggling and detection pulses (both server-authoritative). */
public final class ManaSensing {
    public static final int DETECT_COOLDOWN_TICKS = 60;
    private static final int MAX_ENTRIES = 32;

    private ManaSensing() {}

    public static void toggleConcealment(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        boolean concealing = !data.concealing();
        data.setConcealing(concealing);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            concealing ? ModSounds.MANA_CONCEAL.get() : ModSounds.MANA_RELEASE.get(), SoundSource.PLAYERS, 0.6F, concealing ? 0.8F : 1.2F);
        Messages.actionBar(player, Component.translatable(concealing ? "message.frieren.conceal_on" : "message.frieren.conceal_off",
            Math.round(ManaHelper.concealFraction(data) * 100)));
    }

    /** How well a player hides their mana, on the same scale as {@link ManaSignature#concealLevel()}. */
    public static int playerConcealLevel(MagicData data) {
        if (!data.concealing()) {
            return 0;
        }
        return 1 + (int) (data.concealMastery() / 12.0F) + (data.hasConcealmentCharm() ? 2 : 0);
    }

    public static void detect(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        ServerLevel level = player.level();
        long time = level.getGameTime();
        if (time - data.lastDetectTime() < DETECT_COOLDOWN_TICKS) {
            return;
        }
        data.setLastDetectTime(time);

        int power = ManaHelper.detectionPower(data);
        double radius = ManaHelper.detectionRadius(data);
        List<LivingEntity> nearby = SpellTargeting.livingInRadius(level, player, player.position(), radius,
            entity -> entity instanceof Player || entity instanceof ManaSignature);
        nearby.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));

        List<ManaSensePacket.Entry> entries = new ArrayList<>();
        boolean sawThrough = false;
        for (LivingEntity entity : nearby) {
            if (entries.size() >= MAX_ENTRIES) {
                break;
            }
            if (entity instanceof Player other) {
                MagicData otherData = MagicData.get(other);
                if (otherData == null || other.isSpectator()) {
                    continue;
                }
                int conceal = playerConcealLevel(otherData);
                boolean through = conceal > 0 && power > conceal;
                sawThrough |= through;
                float shown = through ? otherData.maxMana() : ManaHelper.apparentMana(otherData);
                entries.add(new ManaSensePacket.Entry(other.getId(), shown, (byte) 0));
            } else if (entity instanceof ManaSignature signature) {
                int conceal = signature.concealLevel();
                boolean through = conceal > 0 && power > conceal;
                sawThrough |= through;
                float shown = through ? signature.trueMana() : signature.trueMana() * signature.apparentFraction();
                entries.add(new ManaSensePacket.Entry(entity.getId(), shown, signature.senseKind()));
            }
        }

        data.setDetectionMastery(data.detectionMastery() + (sawThrough ? 1.0F : 0.4F));
        FrierenNetwork.sendToPlayer(player, new ManaSensePacket(List.copyOf(entries), sawThrough));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MANA_DETECT.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
        ExamManager.onManaDetection(player, data, nearby, power);
    }
}
