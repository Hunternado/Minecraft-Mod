package com.hunternado.frieren.combat;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Keys of the data-driven damage types in data/frieren/damage_type/. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> SPELL = key("spell");
    public static final ResourceKey<DamageType> ZOLTRAAK = key("zoltraak");
    public static final ResourceKey<DamageType> DEMON_MAGIC = key("demon_magic");
    public static final ResourceKey<DamageType> CURSE = key("curse");
    public static final ResourceKey<DamageType> GUILLOTINE = key("guillotine");

    private ModDamageTypes() {}

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, FrierenIds.id(name));
    }

    public static Holder<DamageType> holder(Level level, ResourceKey<DamageType> key) {
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
    }
}
