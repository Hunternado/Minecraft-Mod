package com.hunternado.frieren.client.fx;

import com.hunternado.frieren.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client-side magic particle toolkit. Sprite sets are captured when the providers are registered (they stay valid
 * across resource reloads), so spell visuals can spawn tinted particles with exact sizes and lifetimes.
 */
public final class MagicFx {
    public enum Family { SPARK, RUNE, PETAL, RING, HEX, STREAK, EMBER, FROST, WISP }

    private static final Map<Family, SpriteSet> SPRITES = new EnumMap<>(Family.class);

    private MagicFx() {}

    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.MANA_SPARK.get(), sprites -> capture(Family.SPARK, sprites, true));
        event.registerSpriteSet(ModParticles.RUNE.get(), sprites -> capture(Family.RUNE, sprites, false));
        event.registerSpriteSet(ModParticles.PETAL.get(), sprites -> capture(Family.PETAL, sprites, false));
        event.registerSpriteSet(ModParticles.MAGIC_RING.get(), sprites -> capture(Family.RING, sprites, true));
        event.registerSpriteSet(ModParticles.HEX.get(), sprites -> capture(Family.HEX, sprites, false));
        event.registerSpriteSet(ModParticles.LIGHT_STREAK.get(), sprites -> capture(Family.STREAK, sprites, false));
        event.registerSpriteSet(ModParticles.EMBER.get(), sprites -> capture(Family.EMBER, sprites, true));
        event.registerSpriteSet(ModParticles.FROST.get(), sprites -> capture(Family.FROST, sprites, false));
        event.registerSpriteSet(ModParticles.DEMON_WISP.get(), sprites -> capture(Family.WISP, sprites, true));
    }

    private static net.minecraft.client.particle.ParticleProvider<net.minecraft.core.particles.SimpleParticleType> capture(
            Family family, SpriteSet sprites, boolean animated) {
        SPRITES.put(family, sprites);
        return MagicParticle.provider(sprites, animated);
    }

    private static boolean animated(Family family) {
        return family == Family.SPARK || family == Family.RING || family == Family.EMBER || family == Family.WISP;
    }

    /** Spawns one particle; silently does nothing before resources are loaded. */
    public static void spawn(Family family, double x, double y, double z, double vx, double vy, double vz, int rgb,
                             float size, int lifetime, float gravity, float spin) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        SpriteSet sprites = SPRITES.get(family);
        if (level == null || sprites == null) {
            return;
        }
        boolean shrink = family != Family.RING;
        minecraft.particleEngine.add(new MagicParticle(level, x, y, z, vx, vy, vz, sprites, rgb, size, lifetime,
            animated(family), gravity, spin, shrink));
    }

    public static void spawn(Family family, Vec3 p, Vec3 v, int rgb, float size, int lifetime) {
        spawn(family, p.x, p.y, p.z, v.x, v.y, v.z, rgb, size, lifetime, 0.0F, 0.0F);
    }

    /** Lighter variant of a colour, for bright cores. */
    public static int brighten(int rgb, float amount) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = (int) (r + (255 - r) * amount);
        g = (int) (g + (255 - g) * amount);
        b = (int) (b + (255 - b) * amount);
        return (r << 16) | (g << 8) | b;
    }

    public static Vec3 randomDirection(RandomSource random) {
        double theta = random.nextDouble() * Math.PI * 2.0D;
        double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);
        return new Vec3(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));
    }
}
