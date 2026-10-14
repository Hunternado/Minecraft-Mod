package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.entity.boss.AuraEntity;
import com.hunternado.frieren.entity.demon.AuraThrallEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frieren and Aura, as it went at Graf Granat's city. When Frieren comes within {@link #TRIGGER_RANGE} blocks (four
 * chunks) of a living Aura, both set their own AI aside and the scene plays out for everyone within 64 blocks:
 * <ol>
 *   <li>Frieren walks up to her; Aura sneers at how little mana the hero's mage has (Frieren is concealing, and a
 *   mana sense shows it); Frieren clears away the headless thralls.</li>
 *   <li>Aura raises the Scales of Obedience. Frieren releases her limiter: a pillar of mana, her signature now
 *   in full view (detect it and see).</li>
 *   <li>The scales tip toward Frieren. "Aura, kill yourself." Aura obeys, and dies by her own guillotine; her loot
 *   drops as usual.</li>
 * </ol>
 * If either of them is removed, killed or carried more than 96 blocks apart, the scene is abandoned and both return to
 * their normal behaviour. Witnesses earn the "Frieren the Slayer" advancement.
 */
public final class AuraDuel {
    public static final double TRIGGER_RANGE = 64.0D;
    private static final double AUDIENCE = 64.0D;
    private static final Map<UUID, AuraDuel> DUELS = new ConcurrentHashMap<>();

    private final ServerLevel level;
    private final UUID frierenId;
    private final UUID auraId;
    private int tick;
    private boolean inPlace;
    private int approachTicks;

    private AuraDuel(ServerLevel level, FrierenEntity frieren, AuraEntity aura) {
        this.level = level;
        this.frierenId = frieren.getUUID();
        this.auraId = aura.getUUID();
    }

    /** Called by Frieren every two seconds: starts the scene when an Aura is within four chunks. */
    public static void tryBegin(ServerLevel level, FrierenEntity frieren) {
        if (frieren.isScripted() || DUELS.values().stream().anyMatch(duel -> duel.frierenId.equals(frieren.getUUID()))) {
            return;
        }
        List<AuraEntity> auras = level.getEntitiesOfClass(AuraEntity.class, new AABB(frieren.blockPosition()).inflate(TRIGGER_RANGE),
            aura -> aura.isAlive() && !aura.isScripted() && !DUELS.containsKey(aura.getUUID()));
        if (auras.isEmpty()) {
            return;
        }
        AuraEntity aura = auras.get(0);
        AuraDuel duel = new AuraDuel(level, frieren, aura);
        DUELS.put(aura.getUUID(), duel);
        frieren.setScripted(true);
        aura.setScripted(true);
    }

    /** Every server tick. */
    public static void tick(MinecraftServer server) {
        if (DUELS.isEmpty()) {
            return;
        }
        Iterator<AuraDuel> iterator = DUELS.values().iterator();
        while (iterator.hasNext()) {
            AuraDuel duel = iterator.next();
            if (!duel.step()) {
                iterator.remove();
            }
        }
    }

    public static void clear() {
        DUELS.clear();
    }

    /** One tick of the scene; false when it is over (finished or abandoned). */
    private boolean step() {
        Entity f = level.getEntity(frierenId);
        Entity a = level.getEntity(auraId);
        if (!(f instanceof FrierenEntity frieren) || !frieren.isAlive()) {
            if (a instanceof AuraEntity aura) {
                abandon(null, aura);
            }
            return false;
        }
        if (!(a instanceof AuraEntity aura) || !aura.isAlive()) {
            if (tick < 470) {
                abandon(frieren, null);
            }
            return tick < 470 ? false : epilogue(frieren);
        }
        if (frieren.distanceToSqr(aura) > 96.0D * 96.0D) {
            abandon(frieren, aura);
            return false;
        }
        face(aura, frieren.position());
        face(frieren, aura.position());
        if (!inPlace) {
            approach(frieren, aura);
            return true;
        }
        tick++;
        Vec3 auraTop = aura.position().add(0.0D, aura.getBbHeight() + 0.8D, 0.0D);
        Vec3 frierenTop = frieren.position().add(0.0D, frieren.getBbHeight() + 0.8D, 0.0D);
        switch (tick) {
            case 1 -> say(aura, "aura.0");
            case 60 -> say(aura, "aura.1");
            case 140 -> {
                say(aura, "aura.2");
                aura.setScalesRaised(true);
                level.playSound(null, aura.getX(), aura.getY(), aura.getZ(), ModSounds.AURA_SCALES.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
            }
            case 200 -> say(frieren, "frieren.0");
            case 220 -> reveal(frieren);
            case 250 -> say(aura, "aura.3");
            case 300 -> say(frieren, "frieren.1");
            case 350 -> {
                narrate("scales_tip");
                SpellFx.scales(level, frierenTop, 0xF2C94C);
                SpellFx.burst(level, frierenTop, 0xF2C94C, 1.5F);
                level.playSound(null, aura.getX(), aura.getY(), aura.getZ(), ModSounds.AURA_SCALES.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case 400 -> {
                say(frieren, "frieren.2");
                frieren.pose(30);
            }
            case 440 -> say(aura, "aura.4");
            case 470 -> {
                aura.setScalesRaised(false);
                SpellFx.beam(level, auraTop.add(0.0D, 2.0D, 0.0D), aura.position().add(0.0D, 0.4D, 0.0D), 0xFF2040, 0.8F, 12,
                    SpellBeamEntity.STYLE_DEMON);
                SpellFx.burst(level, aura.position().add(0.0D, aura.getBbHeight(), 0.0D), 0xFF2040, 1.6F);
                level.playSound(null, aura.getX(), aura.getY(), aura.getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 2.0F, 0.5F);
                aura.setScripted(false);
                aura.hurtServer(level, new DamageSource(ModDamageTypes.holder(level, ModDamageTypes.GUILLOTINE), aura), aura.getMaxHealth() * 10.0F);
                if (aura.isAlive()) {
                    aura.kill(level);
                }
            }
            default -> {
            }
        }
        // While Aura talks down to her, Frieren clears away the headless thralls one by one.
        if (tick > 60 && tick < 200 && tick % 12 == 0) {
            List<AuraThrallEntity> thralls = level.getEntitiesOfClass(AuraThrallEntity.class, new AABB(frieren.blockPosition()).inflate(24.0D),
                Entity::isAlive);
            if (!thralls.isEmpty()) {
                AuraThrallEntity thrall = thralls.get(0);
                MagicCircleEntity.inFront(level, frieren, MagicCircleEntity.STYLE_ARCANE, 0xE8F4FF, 0.8F, 10, 1.0F);
                SpellFx.beam(level, frieren.getEyePosition(), thrall.getEyePosition(), 0xE8F4FF, 0.3F, 10, SpellBeamEntity.STYLE_ZOLTRAAK);
                thrall.hurtServer(level, new DamageSource(ModDamageTypes.holder(level, ModDamageTypes.ZOLTRAAK), frieren), 1000.0F);
                frieren.pose(10);
            }
        }
        if (tick > 140 && tick < 470 && tick % 20 == 0) {
            SpellFx.scales(level, auraTop, 0xD4AF37);
            SpellFx.scales(level, frierenTop, 0xD4AF37);
        }
        return true;
    }

    /** Frieren walks up to about eight blocks; after twenty seconds of trying she simply appears there. */
    private void approach(FrierenEntity frieren, AuraEntity aura) {
        double distance = frieren.distanceTo(aura);
        if (distance <= 9.0D) {
            inPlace = true;
            frieren.getNavigation().stop();
            narrate("begins");
            return;
        }
        if (++approachTicks > 400) {
            Vec3 toward = frieren.position().subtract(aura.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(8.0D);
            Vec3 spot = aura.position().add(toward);
            frieren.teleportTo(spot.x, aura.getY(), spot.z);
            return;
        }
        if (approachTicks % 10 == 1) {
            frieren.getNavigation().moveTo(aura, 1.0D);
        }
    }

    /** The limiter comes off: a pillar of mana, and her signature in full view. */
    private void reveal(FrierenEntity frieren) {
        frieren.setRevealed(true);
        frieren.pose(40);
        Vec3 feet = frieren.position();
        SpellFx.beam(level, feet, feet.add(0.0D, 32.0D, 0.0D), 0x9FD8FF, 1.8F, 60, SpellBeamEntity.STYLE_LIGHT);
        SpellFx.ring(level, feet.add(0.0D, 0.1D, 0.0D), 0x9FD8FF, 8.0F);
        SpellFx.spiral(level, feet, 0xCDEBFF, 8.0F);
        SpellFx.burst(level, feet.add(0.0D, 1.0D, 0.0D), 0xFFFFFF, 3.5F);
        level.playSound(null, frieren.getX(), frieren.getY(), frieren.getZ(), ModSounds.LIMITER.get(), SoundSource.NEUTRAL, 2.0F, 0.9F);
        narrate("reveal");
    }

    private boolean epilogue(FrierenEntity frieren) {
        tick++;
        if (tick == 500) {
            say(frieren, "frieren.3");
            SpellFx.petals(level, frieren.position(), 0xFFFFFF, 3.0F);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(frieren) < AUDIENCE * AUDIENCE) {
                    ModTriggers.milestone(player, "witnessed_slayer");
                }
            }
        }
        if (tick >= 560) {
            frieren.setRevealed(false);
            frieren.setScripted(false);
            return false;
        }
        return true;
    }

    private void abandon(FrierenEntity frieren, AuraEntity aura) {
        if (frieren != null) {
            frieren.setRevealed(false);
            frieren.setScripted(false);
        }
        if (aura != null) {
            aura.setScalesRaised(false);
            aura.setScripted(false);
        }
    }

    private void face(Mob mob, Vec3 at) {
        mob.getLookControl().setLookAt(at.x, at.y + 1.4D, at.z, 30.0F, 30.0F);
    }

    private void say(Entity speaker, String key) {
        Component line = Component.literal("<").append(speaker.getDisplayName()).append("> ")
            .append(Component.translatable("duel.frieren." + key).withStyle(key.equals("frieren.2")
                ? new ChatFormatting[] {ChatFormatting.GOLD, ChatFormatting.BOLD} : new ChatFormatting[] {ChatFormatting.WHITE}));
        broadcast(speaker.position(), line);
    }

    private void narrate(String key) {
        Entity frieren = level.getEntity(frierenId);
        if (frieren != null) {
            broadcast(frieren.position(), Component.translatable("duel.frieren." + key).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
    }

    private void broadcast(Vec3 at, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(at) < AUDIENCE * AUDIENCE) {
                player.sendSystemMessage(message);
            }
        }
    }
}
