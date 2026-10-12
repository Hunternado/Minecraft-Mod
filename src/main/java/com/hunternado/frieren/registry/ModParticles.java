package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Magic particle families. Their sprites are white and tinted per spell on the client (see client/fx/MagicFx),
 * so a handful of types cover every spell colour.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FrierenMod.MODID);

    public static final RegistryObject<SimpleParticleType> MANA_SPARK = register("mana_spark");
    public static final RegistryObject<SimpleParticleType> RUNE = register("rune");
    public static final RegistryObject<SimpleParticleType> PETAL = register("petal");
    public static final RegistryObject<SimpleParticleType> MAGIC_RING = register("magic_ring");
    public static final RegistryObject<SimpleParticleType> HEX = register("hex");
    public static final RegistryObject<SimpleParticleType> LIGHT_STREAK = register("light_streak");
    public static final RegistryObject<SimpleParticleType> EMBER = register("ember");
    public static final RegistryObject<SimpleParticleType> FROST = register("frost");
    public static final RegistryObject<SimpleParticleType> DEMON_WISP = register("demon_wisp");

    private ModParticles() {}

    private static RegistryObject<SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }
}
