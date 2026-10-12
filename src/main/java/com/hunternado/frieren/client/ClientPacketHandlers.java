package com.hunternado.frieren.client;

import com.hunternado.frieren.client.fx.MagicFx;
import com.hunternado.frieren.client.gui.ExaminerScreen;
import com.hunternado.frieren.client.gui.TradeScreen;
import com.hunternado.frieren.network.packet.ExaminerScreenPacket;
import com.hunternado.frieren.network.packet.ManaSensePacket;
import com.hunternado.frieren.network.packet.SpellFxPacket;
import com.hunternado.frieren.network.packet.SyncMagicDataPacket;
import com.hunternado.frieren.network.packet.SyncManaPacket;
import com.hunternado.frieren.network.packet.SyncSpellsPacket;
import com.hunternado.frieren.network.packet.TradeScreenPacket;
import com.hunternado.frieren.spell.SpellManager;
import com.hunternado.frieren.util.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Handlers for clientbound packets. Only referenced from clientbound handler lambdas in FrierenNetwork,
 * which never run on a dedicated server.
 */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void syncMagicData(SyncMagicDataPacket packet) {
        ClientMagicCache.data().load(packet.data());
    }

    public static void syncMana(SyncManaPacket packet) {
        ClientMagicCache.data().setCachedMaxMana(packet.maxMana());
        ClientMagicCache.data().setMana(packet.mana());
    }

    public static void syncSpells(SyncSpellsPacket packet) {
        SpellManager.setClientDefinitions(packet.spells());
    }

    public static void manaSense(ManaSensePacket packet) {
        ClientMagicCache.setSensed(packet);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            Messages.actionBar(minecraft.player, Component.translatable(packet.sawThroughSomething()
                ? "message.frieren.sense_through" : "message.frieren.sense_count", packet.entries().size()));
        }
    }

    public static void openExaminer(ExaminerScreenPacket packet) {
        Minecraft.getInstance().gui.setScreen(new ExaminerScreen(packet.entityId()));
    }

    public static void openTrades(TradeScreenPacket packet) {
        Minecraft.getInstance().gui.setScreen(new TradeScreen(packet));
    }

    /** Called every client tick: outlines sensed entities with particles. Cheap (a handful per tick). */
    public static void tickSenseMarks() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.player.tickCount % 4 != 0) {
            return;
        }
        for (ManaSensePacket.Entry entry : ClientMagicCache.activeSense()) {
            Entity entity = level.getEntity(entry.entityId());
            if (entity == null) {
                continue;
            }
            int color = switch (entry.kind()) {
                case 0 -> 0x7FD4FF;
                case 1 -> 0xE0405A;
                case 2 -> 0xB8F0A0;
                default -> 0xC9A0FF;
            };
            ColorParticleOption tint = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(255, color));
            int count = 1 + Math.min(4, (int) (entry.apparentMana() / 100.0F));
            RandomSource random = level.getRandom();
            for (int i = 0; i < count; i++) {
                level.addParticle(tint, entity.getX() + (random.nextDouble() - 0.5D) * entity.getBbWidth(),
                    entity.getY() + random.nextDouble() * entity.getBbHeight(),
                    entity.getZ() + (random.nextDouble() - 0.5D) * entity.getBbWidth(), 0.0D, 0.05D, 0.0D);
            }
        }
    }

    public static void spellFx(SpellFxPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 a = new Vec3(packet.x1(), packet.y1(), packet.z1());
        Vec3 b = new Vec3(packet.x2(), packet.y2(), packet.z2());
        int color = packet.color();
        int bright = MagicFx.brighten(color, 0.6F);
        switch (packet.type()) {
            case SpellFxPacket.BEAM -> {
                // The beam itself is a rendered entity; add drifting sparks along it and a flash at the impact.
                double length = a.distanceTo(b);
                int steps = Math.min(40, Math.max(2, (int) (length * 1.2D)));
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = a.lerp(b, i / (double) steps);
                    Vec3 v = MagicFx.randomDirection(random).scale(0.02D);
                    MagicFx.spawn(MagicFx.Family.SPARK, p, v, i % 2 == 0 ? color : bright, 0.08F, 10 + random.nextInt(8));
                }
                impact(b, color, bright, 0.7F, random);
            }
            case SpellFxPacket.BURST -> impact(a, color, bright, Math.max(0.4F, packet.param()), random);
            case SpellFxPacket.RING -> {
                float radius = packet.param();
                int count = Math.min(72, (int) (radius * 10.0F) + 10);
                for (int i = 0; i < count; i++) {
                    double angle = i * Math.PI * 2.0D / count;
                    double x = a.x + Math.cos(angle) * radius;
                    double z = a.z + Math.sin(angle) * radius;
                    MagicFx.spawn(MagicFx.Family.SPARK, x, a.y + 0.1D, z, 0.0D, 0.03D + random.nextDouble() * 0.03D, 0.0D,
                        i % 3 == 0 ? bright : color, 0.12F, 18 + random.nextInt(10), 0.0F, 0.0F);
                    if (i % 6 == 0) {
                        MagicFx.spawn(MagicFx.Family.RUNE, x, a.y + 0.4D, z, 0.0D, 0.04D, 0.0D, color, 0.16F, 26, 0.0F, 0.05F);
                    }
                }
            }
            case SpellFxPacket.CONE -> {
                Vec3 direction = b.subtract(a);
                double range = direction.length();
                Vec3 forward = direction.normalize();
                boolean fire = packet.param() < 0.5F;
                int count = fire ? 70 : 24;
                for (int i = 0; i < count; i++) {
                    double distance = random.nextDouble() * range;
                    Vec3 jitter = MagicFx.randomDirection(random).scale(distance * (fire ? 0.45D : 0.3D));
                    Vec3 p = a.add(forward.scale(distance)).add(jitter);
                    Vec3 v = forward.scale(fire ? 0.12D + random.nextDouble() * 0.1D : 0.3D);
                    if (fire) {
                        int ember = random.nextBoolean() ? color : MagicFx.brighten(color, 0.4F);
                        MagicFx.spawn(MagicFx.Family.EMBER, p.x, p.y, p.z, v.x, v.y + 0.02D, v.z, ember, 0.22F + random.nextFloat() * 0.18F,
                            14 + random.nextInt(10), -0.01F, 0.1F);
                    } else {
                        MagicFx.spawn(MagicFx.Family.STREAK, p, v, bright, 0.25F, 6 + random.nextInt(4));
                    }
                }
            }
            case SpellFxPacket.HEXAGON -> {
                float radius = packet.param();
                for (int i = 0; i < 18; i++) {
                    Vec3 dir = MagicFx.randomDirection(random);
                    Vec3 p = a.add(dir.scale(radius));
                    MagicFx.spawn(MagicFx.Family.HEX, p, dir.scale(0.02D), i % 2 == 0 ? color : bright, 0.18F, 10 + random.nextInt(6));
                }
            }
            case SpellFxPacket.SPIRAL -> {
                float height = packet.param();
                for (int i = 0; i < 48; i++) {
                    double t = i / 48.0D;
                    double angle = t * Math.PI * 6.0D;
                    double r = 0.9D - t * 0.4D;
                    MagicFx.spawn(MagicFx.Family.SPARK, a.x + Math.cos(angle) * r, a.y + t * height, a.z + Math.sin(angle) * r,
                        0.0D, 0.02D, 0.0D, i % 2 == 0 ? color : bright, 0.11F, 20 + random.nextInt(8), 0.0F, 0.0F);
                    if (i % 8 == 0) {
                        MagicFx.spawn(MagicFx.Family.RUNE, a.x + Math.cos(angle) * r, a.y + t * height, a.z + Math.sin(angle) * r,
                            0.0D, 0.03D, 0.0D, color, 0.15F, 24, 0.0F, 0.04F);
                    }
                }
            }
            case SpellFxPacket.SCALES -> {
                int gold = 0xF2C94C;
                for (int i = 0; i < 14; i++) {
                    double t = i / 13.0D - 0.5D;
                    MagicFx.spawn(MagicFx.Family.SPARK, a.x + t * 1.6D, a.y, a.z, 0.0D, 0.0D, 0.0D, gold, 0.1F, 24, 0.0F, 0.0F);
                }
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI * 2.0D / 8.0D;
                        MagicFx.spawn(MagicFx.Family.SPARK, a.x + side * 0.8D + Math.cos(angle) * 0.3D, a.y - 0.5D,
                            a.z + Math.sin(angle) * 0.3D, 0.0D, 0.0D, 0.0D, color, 0.1F, 24, 0.0F, 0.0F);
                    }
                }
                MagicFx.spawn(MagicFx.Family.RING, a.x, a.y + 0.2D, a.z, 0.0D, 0.0D, 0.0D, gold, 0.8F, 18, 0.0F, 0.0F);
            }
            case SpellFxPacket.PETALS -> {
                float radius = Math.max(1.0F, packet.param());
                int count = Math.min(80, (int) (radius * 12.0F));
                int[] colors = {0xFFFFFF, 0xFFC0DA, 0xA8D8FF, color};
                for (int i = 0; i < count; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double r = random.nextDouble() * radius;
                    MagicFx.spawn(MagicFx.Family.PETAL, a.x + Math.cos(angle) * r, a.y + 0.2D + random.nextDouble(), a.z + Math.sin(angle) * r,
                        (random.nextDouble() - 0.5D) * 0.04D, 0.05D + random.nextDouble() * 0.05D, (random.nextDouble() - 0.5D) * 0.04D,
                        colors[random.nextInt(colors.length)], 0.13F, 40 + random.nextInt(30), 0.002F, 0.06F);
                }
            }
            case SpellFxPacket.STREAKS -> {
                float radius = Math.max(1.0F, packet.param());
                for (int i = 0; i < 26; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double r = random.nextDouble() * radius;
                    double x = a.x + Math.cos(angle) * r;
                    double z = a.z + Math.sin(angle) * r;
                    MagicFx.spawn(MagicFx.Family.STREAK, x, a.y + 6.0D + random.nextDouble() * 4.0D, z, 0.0D, -1.2D, 0.0D, bright,
                        0.35F, 7, 0.0F, 0.0F);
                }
            }
            case SpellFxPacket.FROST -> {
                float radius = Math.max(0.5F, packet.param());
                for (int i = 0; i < 40; i++) {
                    Vec3 dir = MagicFx.randomDirection(random);
                    Vec3 p = a.add(dir.scale(random.nextDouble() * radius));
                    MagicFx.spawn(MagicFx.Family.FROST, p, dir.scale(0.03D), i % 3 == 0 ? 0xFFFFFF : color, 0.14F, 26 + random.nextInt(14));
                }
            }
            case SpellFxPacket.WISPS -> {
                float radius = Math.max(0.5F, packet.param());
                for (int i = 0; i < 30; i++) {
                    Vec3 dir = MagicFx.randomDirection(random);
                    Vec3 p = a.add(dir.scale(random.nextDouble() * radius));
                    MagicFx.spawn(MagicFx.Family.WISP, p.x, p.y, p.z, dir.x * 0.02D, 0.03D, dir.z * 0.02D, color,
                        0.35F + random.nextFloat() * 0.3F, 24 + random.nextInt(14), -0.002F, 0.02F);
                }
            }
            default -> {
            }
        }
    }

    /** A flash of sparks, an expanding ring and a few runes. */
    private static void impact(Vec3 at, int color, int bright, float scale, RandomSource random) {
        int count = Math.min(40, 10 + (int) (scale * 14.0F));
        for (int i = 0; i < count; i++) {
            Vec3 v = MagicFx.randomDirection(random).scale(0.05D + random.nextDouble() * 0.12D * scale);
            MagicFx.spawn(MagicFx.Family.SPARK, at, v, i % 2 == 0 ? color : bright, 0.1F + random.nextFloat() * 0.08F,
                12 + random.nextInt(10));
        }
        MagicFx.spawn(MagicFx.Family.RING, at.x, at.y, at.z, 0.0D, 0.0D, 0.0D, bright, 0.6F + scale * 0.5F, 10, 0.0F, 0.0F);
        for (int i = 0; i < 3; i++) {
            Vec3 v = MagicFx.randomDirection(random).scale(0.04D);
            MagicFx.spawn(MagicFx.Family.RUNE, at.x, at.y, at.z, v.x, v.y + 0.03D, v.z, color, 0.14F, 20, 0.0F, 0.08F);
        }
    }
}
