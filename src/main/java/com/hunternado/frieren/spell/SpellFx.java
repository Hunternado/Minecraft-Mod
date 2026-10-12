package com.hunternado.frieren.spell;

import com.hunternado.frieren.entity.fx.BarrierShellEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.magic.SpellSchool;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.SpellFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side visual/audio helpers. Each call sends one compact {@link SpellFxPacket} to players within
 * {@link #FX_RANGE} blocks; the client expands it into particles.
 */
public final class SpellFx {
    public static final double FX_RANGE = 64.0D;

    private SpellFx() {}

    /** A rendered beam (SpellBeamEntity) plus sparks along it and at the impact point. */
    public static void beam(ServerLevel level, Vec3 from, Vec3 to, int color) {
        beam(level, from, to, color, 0.32F, 10, SpellBeamEntity.STYLE_ZOLTRAAK);
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, int color, float width, int life, byte style) {
        SpellBeamEntity.fire(level, from, to, color, width, life, style);
        send(level, from, SpellFxPacket.BEAM, from, to, color, 0.0F);
    }

    /** A magic circle in front of the caster, styled for the spell; follows them for {@code life} ticks. */
    public static void castCircle(ServerLevel level, LivingEntity caster, SpellDefinition spell, int life) {
        MagicCircleEntity.inFront(level, caster, circleStyle(spell), spell.color(), 0.9F + 0.1F * spell.rarity().ordinal(),
            life, 1.1F);
    }

    public static void groundCircle(ServerLevel level, Vec3 center, SpellDefinition spell, float radius, int life) {
        MagicCircleEntity.onGround(level, center, circleStyle(spell), spell.color(), radius, life);
    }

    public static void groundCircle(ServerLevel level, Vec3 center, byte style, int color, float radius, int life) {
        MagicCircleEntity.onGround(level, center, style, color, radius, life);
    }

    /** Defensive Magic's hexagonal shell flashing around a defender. */
    public static void shell(ServerLevel level, Entity owner, int color) {
        BarrierShellEntity.flash(level, owner, color, 12);
        send(level, owner.position(), SpellFxPacket.HEXAGON, owner.position().add(0.0D, owner.getBbHeight() / 2.0D, 0.0D),
            owner.position(), color, Math.max(owner.getBbWidth(), owner.getBbHeight()) * 0.75F);
    }

    public static void petals(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.PETALS, center, center, color, radius);
    }

    public static void streaks(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.STREAKS, center, center, color, radius);
    }

    public static void frost(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.FROST, center, center, color, radius);
    }

    public static void wisps(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.WISPS, center, center, color, radius);
    }

    public static byte circleStyle(SpellDefinition spell) {
        if (spell.rarity() == SpellRarity.DEMON || spell.rarity() == SpellRarity.FORBIDDEN) {
            return MagicCircleEntity.STYLE_DEMON;
        }
        if (spell.id().getPath().contains("flower") || spell.id().getPath().contains("harvest")) {
            return MagicCircleEntity.STYLE_FLORA;
        }
        if (spell.school() == SpellSchool.DEFENSIVE) {
            return MagicCircleEntity.STYLE_HOLY;
        }
        return MagicCircleEntity.STYLE_ARCANE;
    }

    public static void burst(ServerLevel level, Vec3 at, int color, float radius) {
        send(level, at, SpellFxPacket.BURST, at, at, color, radius);
    }

    public static void ring(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.RING, center, center, color, radius);
    }

    /** {@code style}: 0 = hellfire, 1 = cutting arc. */
    public static void cone(ServerLevel level, Vec3 origin, Vec3 direction, int color, float range, int style) {
        Vec3 end = origin.add(direction.normalize().scale(range));
        send(level, origin, SpellFxPacket.CONE, origin, end, color, style);
    }

    public static void hexagon(ServerLevel level, Vec3 center, int color, float radius) {
        send(level, center, SpellFxPacket.HEXAGON, center, center, color, radius);
    }

    public static void spiral(ServerLevel level, Vec3 base, int color, float height) {
        send(level, base, SpellFxPacket.SPIRAL, base, base, color, height);
    }

    public static void scales(ServerLevel level, Vec3 at, int color) {
        send(level, at, SpellFxPacket.SCALES, at, at, color, 0.0F);
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void send(ServerLevel level, Vec3 near, byte type, Vec3 a, Vec3 b, int color, float param) {
        FrierenNetwork.sendNear(level, near, FX_RANGE, new SpellFxPacket(type, a.x, a.y, a.z, b.x, b.y, b.z, color, param));
    }
}
