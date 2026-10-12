package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.effect.FrierenMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FrierenMod.MODID);

    /** Lost Aura's weighing (or hit by an obedience curse): rooted, weakened and unable to cast. */
    public static final RegistryObject<MobEffect> ENTHRALLED = MOB_EFFECTS.register("enthralled", () ->
        new FrierenMobEffect(MobEffectCategory.HARMFUL, 0x7A1E2C)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, FrierenIds.id("effect.enthralled.speed"), -0.85D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, FrierenIds.id("effect.enthralled.damage"), -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Diagolze: turned into a golden statue. Cannot move or deal damage. */
    public static final RegistryObject<MobEffect> GILDED = MOB_EFFECTS.register("gilded", () ->
        new FrierenMobEffect(MobEffectCategory.HARMFUL, 0xFFD24A)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, FrierenIds.id("effect.gilded.speed"), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, FrierenIds.id("effect.gilded.knockback"), 1.0D, AttributeModifier.Operation.ADD_VALUE));

    /** Granted by Barrier Dome: a visible marker that the bearer is inside allied defensive magic. */
    public static final RegistryObject<MobEffect> WARDED = MOB_EFFECTS.register("warded", () ->
        new FrierenMobEffect(MobEffectCategory.BENEFICIAL, 0x7FD4FF));

    /** Sorganeil / Draht's wire snare: held perfectly still. Cannot walk, jump or fly. */
    public static final RegistryObject<MobEffect> BOUND = MOB_EFFECTS.register("bound", () ->
        new FrierenMobEffect(MobEffectCategory.HARMFUL, 0xC8D8FF)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, FrierenIds.id("effect.bound.speed"), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, FrierenIds.id("effect.bound.flying"), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, FrierenIds.id("effect.bound.jump"), -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, FrierenIds.id("effect.bound.knockback"), 1.0D, AttributeModifier.Operation.ADD_VALUE));

    /** Reflection barrier: projectiles and spells striking the bearer are turned back at their source. */
    public static final RegistryObject<MobEffect> REFLECTING = MOB_EFFECTS.register("reflecting", () ->
        new FrierenMobEffect(MobEffectCategory.BENEFICIAL, 0xE6F4FF));

    private ModEffects() {}

    public static Holder<MobEffect> holder(RegistryObject<MobEffect> effect) {
        return effect.getHolder().orElseThrow(() -> new IllegalStateException("Effect not registered: " + effect.getId()));
    }
}
