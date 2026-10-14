package com.hunternado.frieren.mana;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.boss.FrierenBoss;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.exam.ExamManager;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import com.hunternado.frieren.registry.ModEffects;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import com.hunternado.frieren.util.Hostility;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Mana concealment, its release, and detection (all server-authoritative).
 * <ul>
 *   <li><b>Concealment</b> hides part of your signature; demons judge strength by what they can see, so a concealed
 *   mage looks like easy prey (DemonEntity).</li>
 *   <li><b>Limiter release</b>: dropping concealment after holding it for at least 30 seconds is Frieren's reveal.
 *   Your true mana floods out in a visible pillar, ordinary demons weaker than you are frightened and flee, and you are
 *   {@link ModEffects#UNLEASHED} (+20% to +40% spell power for 10 to 30 seconds, more the longer and better you hid).</li>
 *   <li><b>Detection pulse</b>: lists signatures within your detection radius (24 blocks, +4 per rank, + up to 16 with
 *   detection mastery). Concealment you cannot see through can still be noticed, as a gap where mana should be, once
 *   your detection mastery reaches 30 or your power comes within two of the concealment.</li>
 *   <li><b>Passive sensing</b>: from Third Class or 25 detection mastery, an overwhelming signature (three times your
 *   mana or more) within one and a half detection radii makes itself felt, with its direction.</li>
 * </ul>
 */
public final class ManaSensing {
    public static final int DETECT_COOLDOWN_TICKS = 60;
    /** Concealment held at least this long turns its release into a limiter release. */
    public static final int LIMITER_MIN_TICKS = 600;
    private static final int MAX_ENTRIES = 32;
    private static final String[] COMPASS = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};

    private ManaSensing() {}

    public static void toggleConcealment(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        long time = player.level().getGameTime();
        boolean concealing = !data.concealing();
        long since = data.concealedSince();
        data.setConcealing(concealing);
        data.setConcealedSince(concealing ? time : -1L);
        if (!concealing && since >= 0L && time - since >= LIMITER_MIN_TICKS) {
            releaseLimiter(player, data, time - since);
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            concealing ? ModSounds.MANA_CONCEAL.get() : ModSounds.MANA_RELEASE.get(), SoundSource.PLAYERS, 0.6F, concealing ? 0.8F : 1.2F);
        Messages.actionBar(player, Component.translatable(concealing ? "message.frieren.conceal_on" : "message.frieren.conceal_off",
            Math.round(ManaHelper.concealFraction(data) * 100)));
    }

    /** Frieren's reveal: see the class comment. */
    public static void releaseLimiter(ServerPlayer player, MagicData data, long concealedTicks) {
        ServerLevel level = player.level();
        float strength = Math.min(1.0F, concealedTicks / 6000.0F) * (0.5F + 0.5F * data.concealMastery() / 100.0F);
        int duration = 200 + Math.round(400.0F * strength);
        int amplifier = 1 + Math.round(3.0F * strength);
        player.addEffect(new MobEffectInstance(ModEffects.holder(ModEffects.UNLEASHED), duration, amplifier));

        int frightened = 0;
        for (DemonEntity demon : level.getEntitiesOfClass(DemonEntity.class, new AABB(player.blockPosition()).inflate(16.0D),
                demon -> !(demon instanceof FrierenBoss) && demon.trueMana() < data.maxMana())) {
            demon.frighten(player, 100 + Math.round(200.0F * strength));
            frightened++;
        }
        for (LivingEntity foe : SpellTargeting.livingInRadius(level, player, player.position(), 5.0D, Hostility::isHostile)) {
            Vec3 away = foe.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(1.2D).add(0.0D, 0.4D, 0.0D);
            foe.setDeltaMovement(foe.getDeltaMovement().add(away));
            foe.needsSync = true;
        }

        Vec3 feet = player.position();
        SpellFx.beam(level, feet, feet.add(0.0D, 28.0D, 0.0D), 0x9FD8FF, 1.4F, 34, SpellBeamEntity.STYLE_LIGHT);
        SpellFx.ring(level, feet.add(0.0D, 0.1D, 0.0D), 0x9FD8FF, 6.0F);
        SpellFx.spiral(level, feet, 0xCDEBFF, 6.0F);
        SpellFx.burst(level, feet.add(0.0D, 1.0D, 0.0D), 0xFFFFFF, 2.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LIMITER.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        Messages.chat(player, Component.translatable("message.frieren.limiter_release", data.maxMana(),
            Math.round(amplifier * 10.0F + 10.0F), duration / 20).withStyle(ChatFormatting.AQUA));
        if (frightened > 0) {
            Messages.actionBar(player, Component.translatable("message.frieren.limiter_frightened", frightened).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        for (ServerPlayer other : level.players()) {
            if (other != player && other.distanceToSqr(player) < 64.0D * 64.0D) {
                Messages.chat(other, Component.translatable("message.frieren.limiter_witnessed", player.getDisplayName())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        ModTriggers.milestone(player, "limiter_release");
        SerieEntity.onLimiterRelease(player);
    }

    /** How well a player hides their mana, on the same scale as {@link ManaSignature#concealLevel()}. */
    public static int playerConcealLevel(MagicData data) {
        if (!data.concealing() || data.flaring()) {
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
            int conceal;
            float shown;
            byte kind;
            if (entity instanceof Player other) {
                MagicData otherData = MagicData.get(other);
                if (otherData == null || other.isSpectator()) {
                    continue;
                }
                conceal = playerConcealLevel(otherData);
                boolean through = conceal > 0 && power > conceal;
                shown = through ? otherData.maxMana() : ManaHelper.apparentMana(otherData);
                kind = 0;
            } else if (entity instanceof ManaSignature signature) {
                conceal = signature.concealLevel();
                boolean through = conceal > 0 && power > conceal;
                shown = through ? signature.trueMana() : signature.trueMana() * signature.apparentFraction();
                kind = signature.senseKind();
            } else {
                continue;
            }
            boolean through = conceal > 0 && power > conceal;
            sawThrough |= through;
            byte flags = 0;
            if (through) {
                flags |= ManaSensePacket.FLAG_THROUGH;
            } else if (conceal > 0 && (data.detectionMastery() >= 30.0F || power >= conceal - 2)) {
                flags |= ManaSensePacket.FLAG_NOTICED;
            }
            if (entity instanceof FrierenBoss) {
                flags |= ManaSensePacket.FLAG_BOSS;
            }
            entries.add(new ManaSensePacket.Entry(entity.getId(), shown, kind, flags));
        }

        data.setDetectionMastery(data.detectionMastery() + (sawThrough ? 1.0F : 0.4F));
        FrierenNetwork.sendToPlayer(player, new ManaSensePacket(List.copyOf(entries), sawThrough, data.maxMana()));
        SpellFx.ring(level, player.position().add(0.0D, 0.2D, 0.0D), 0x7FD4FF, 3.0F);
        SpellFx.ring(level, player.position().add(0.0D, 0.2D, 0.0D), 0x7FD4FF, 7.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MANA_DETECT.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
        ExamManager.onManaDetection(player, data, nearby, power);
    }

    /** Every two seconds: an overwhelming signature makes itself felt to a trained mage (see the class comment). */
    public static void passiveSense(ServerPlayer player, MagicData data, long time) {
        if (data.detectionMastery() < 25.0F && !data.rank().atLeast(MageRank.THIRD_CLASS)) {
            return;
        }
        ServerLevel level = player.level();
        int power = ManaHelper.detectionPower(data);
        double radius = ManaHelper.detectionRadius(data) * 1.5D;
        LivingEntity strongest = null;
        float strongestMana = data.maxMana() * 3.0F;
        for (LivingEntity entity : SpellTargeting.livingInRadius(level, player, player.position(), radius,
                candidate -> candidate instanceof ManaSignature)) {
            ManaSignature signature = (ManaSignature) entity;
            int conceal = signature.concealLevel();
            float shown = conceal > 0 && power > conceal ? signature.trueMana() : signature.trueMana() * signature.apparentFraction();
            if (shown >= strongestMana) {
                strongest = entity;
                strongestMana = shown;
            }
        }
        if (strongest == null) {
            return;
        }
        if (strongest.getId() == data.lastPassiveSenseId() && time - data.lastPassiveSenseTime() < 1200L) {
            return;
        }
        data.setLastPassiveSense(time, strongest.getId());
        double dx = strongest.getX() - player.getX();
        double dz = strongest.getZ() - player.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        int octant = Math.floorMod(Math.round(Mth.wrapDegrees(yaw) / 45.0F), 8);
        Component direction = Component.translatable("direction.frieren." + COMPASS[octant]);
        Messages.actionBar(player, Component.translatable("message.frieren.sense_overwhelming", direction,
            Math.round(Math.sqrt(dx * dx + dz * dz))).withStyle(ChatFormatting.DARK_RED));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MANA_DETECT.get(), SoundSource.PLAYERS, 0.7F, 0.5F);
    }
}
