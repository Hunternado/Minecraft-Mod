package com.hunternado.frieren.client;

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
        ParticleOptions tint = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(255, packet.color()));
        switch (packet.type()) {
            case SpellFxPacket.BEAM -> {
                double length = a.distanceTo(b);
                int steps = Math.min(160, Math.max(2, (int) (length * 3.0D)));
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = a.lerp(b, i / (double) steps);
                    level.addParticle(tint, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
                    if (i % 3 == 0) {
                        level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
            case SpellFxPacket.BURST -> {
                int count = Math.min(48, 8 + (int) (packet.param() * 10.0F));
                for (int i = 0; i < count; i++) {
                    Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).normalize();
                    double speed = 0.05D + random.nextDouble() * 0.1D * Math.max(0.5F, packet.param());
                    level.addParticle(tint, a.x, a.y, a.z, dir.x * speed, dir.y * speed, dir.z * speed);
                }
                level.addParticle(ParticleTypes.END_ROD, a.x, a.y, a.z, 0.0D, 0.0D, 0.0D);
            }
            case SpellFxPacket.RING -> {
                float radius = packet.param();
                int count = Math.min(96, (int) (radius * 12.0F) + 8);
                for (int i = 0; i < count; i++) {
                    double angle = i * Math.PI * 2.0D / count;
                    level.addParticle(tint, a.x + Math.cos(angle) * radius, a.y, a.z + Math.sin(angle) * radius, 0.0D, 0.02D, 0.0D);
                }
            }
            case SpellFxPacket.CONE -> {
                Vec3 direction = b.subtract(a);
                double range = direction.length();
                Vec3 forward = direction.normalize();
                ParticleOptions particle = packet.param() < 0.5F ? ParticleTypes.FLAME : ParticleTypes.SWEEP_ATTACK;
                int count = packet.param() < 0.5F ? 60 : 8;
                for (int i = 0; i < count; i++) {
                    double distance = random.nextDouble() * range;
                    Vec3 jitter = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D)
                        .scale(distance * 0.5D);
                    Vec3 p = a.add(forward.scale(distance)).add(jitter);
                    level.addParticle(particle, p.x, p.y, p.z, forward.x * 0.1D, forward.y * 0.1D, forward.z * 0.1D);
                }
            }
            case SpellFxPacket.HEXAGON -> {
                float radius = packet.param();
                for (int side = 0; side < 6; side++) {
                    double a0 = side * Math.PI / 3.0D;
                    double a1 = (side + 1) * Math.PI / 3.0D;
                    for (int i = 0; i < 5; i++) {
                        double t = i / 5.0D;
                        double angle = a0 + (a1 - a0) * t;
                        double r = radius * (Math.cos(Math.PI / 6.0D) / Math.cos(angle - a0 - Math.PI / 6.0D));
                        level.addParticle(tint, a.x + Math.cos(angle) * r, a.y + (random.nextDouble() - 0.5D) * 1.2D,
                            a.z + Math.sin(angle) * r, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
            case SpellFxPacket.SPIRAL -> {
                float height = packet.param();
                for (int i = 0; i < 40; i++) {
                    double t = i / 40.0D;
                    double angle = t * Math.PI * 6.0D;
                    level.addParticle(tint, a.x + Math.cos(angle) * 0.8D, a.y + t * height, a.z + Math.sin(angle) * 0.8D, 0.0D, 0.02D, 0.0D);
                }
            }
            case SpellFxPacket.SCALES -> {
                for (int i = 0; i < 12; i++) {
                    double t = i / 11.0D - 0.5D;
                    level.addParticle(tint, a.x + t * 1.6D, a.y, a.z, 0.0D, 0.0D, 0.0D);
                }
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 6; i++) {
                        double angle = i * Math.PI * 2.0D / 6.0D;
                        level.addParticle(tint, a.x + side * 0.8D + Math.cos(angle) * 0.3D, a.y - 0.5D, a.z + Math.sin(angle) * 0.3D,
                            0.0D, 0.0D, 0.0D);
                    }
                }
                level.addParticle(ParticleTypes.END_ROD, a.x, a.y + 0.2D, a.z, 0.0D, 0.0D, 0.0D);
            }
            default -> {
            }
        }
    }
}
