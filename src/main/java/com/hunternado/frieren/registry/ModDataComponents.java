package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FrierenMod.MODID);

    /** Spell taught by a grimoire. Absent = "unidentified", resolved to a random spell of the grimoire's rarity on first read. */
    public static final RegistryObject<DataComponentType<Identifier>> GRIMOIRE_SPELL = DATA_COMPONENTS.register("grimoire_spell", () ->
        DataComponentType.<Identifier>builder()
            .persistent(Identifier.CODEC)
            .networkSynchronized(Identifier.STREAM_CODEC)
            .build());

    /** Whether a Stille Cage currently holds a Stille. */
    public static final RegistryObject<DataComponentType<Boolean>> CAGED_STILLE = DATA_COMPONENTS.register("caged_stille", () ->
        DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build());

    private ModDataComponents() {}
}
