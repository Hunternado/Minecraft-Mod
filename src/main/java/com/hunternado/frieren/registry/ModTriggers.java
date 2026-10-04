package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.advancement.MilestoneTrigger;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES.key(), FrierenMod.MODID);

    public static final RegistryObject<MilestoneTrigger> MILESTONE = TRIGGERS.register("milestone", MilestoneTrigger::new);

    private ModTriggers() {}

    /** Convenience used across gameplay code. */
    public static void milestone(ServerPlayer player, String milestone) {
        MILESTONE.get().trigger(player, milestone);
    }
}
