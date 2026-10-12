package com.hunternado.frieren.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/**
 * Full-bright, tinted, fading magic particle used by every custom family (sparks, runes, petals, rings, ...).
 * Velocity is set exactly (the base particle constructor's random jitter is bypassed).
 */
public class MagicParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final boolean animated;
    private final float startSize;
    private final float spin;
    private final boolean shrink;

    public MagicParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites,
                         int rgb, float size, int lifetime, boolean animated, float gravity, float spin, boolean shrink) {
        super(level, x, y, z, animated ? sprites.first() : sprites.get(level.getRandom()));
        this.sprites = sprites;
        this.animated = animated;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.rCol = ((rgb >> 16) & 0xFF) / 255.0F;
        this.gCol = ((rgb >> 8) & 0xFF) / 255.0F;
        this.bCol = (rgb & 0xFF) / 255.0F;
        this.quadSize = size;
        this.startSize = size;
        this.lifetime = Math.max(2, lifetime);
        this.gravity = gravity;
        this.friction = 0.94F;
        this.hasPhysics = false;
        this.spin = spin;
        this.shrink = shrink;
        this.roll = level.getRandom().nextFloat() * 6.2831855F;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        super.tick();
        float progress = this.age / (float) this.lifetime;
        if (animated) {
            this.setSpriteFromAge(sprites);
        }
        this.oRoll = this.roll;
        this.roll += spin;
        this.setAlpha(progress < 0.7F ? 1.0F : Math.max(0.0F, 1.0F - (progress - 0.7F) / 0.3F));
        if (shrink) {
            this.quadSize = startSize * (1.0F - 0.6F * progress);
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    /** Default provider for {@code /particle} and data-driven use: white, gently drifting. */
    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, boolean animated) {
        return (SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                RandomSource random) -> new MagicParticle(level, x, y, z, vx, vy, vz, sprites, 0xFFFFFF, 0.15F,
            20 + random.nextInt(10), animated, 0.0F, 0.0F, true);
    }
}
