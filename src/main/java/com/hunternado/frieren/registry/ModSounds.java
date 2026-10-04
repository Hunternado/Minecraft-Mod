package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Mod sound events. Every event is defined in assets/frieren/sounds.json and currently delegates to vanilla
 * sound events, so no audio files ship with the mod; a resource pack can replace them individually.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FrierenMod.MODID);

    public static final RegistryObject<SoundEvent> SPELL_CAST = register("spell.cast");
    public static final RegistryObject<SoundEvent> SPELL_FAIL = register("spell.fail");
    public static final RegistryObject<SoundEvent> ZOLTRAAK = register("spell.zoltraak");
    public static final RegistryObject<SoundEvent> BARRIER_HIT = register("spell.barrier_hit");
    public static final RegistryObject<SoundEvent> BARRIER_BREAK = register("spell.barrier_break");
    public static final RegistryObject<SoundEvent> HEAL = register("spell.heal");
    public static final RegistryObject<SoundEvent> FLOWER_BLOOM = register("spell.flower_bloom");
    public static final RegistryObject<SoundEvent> MANA_CONCEAL = register("mana.conceal");
    public static final RegistryObject<SoundEvent> MANA_RELEASE = register("mana.release");
    public static final RegistryObject<SoundEvent> MANA_DETECT = register("mana.detect");
    public static final RegistryObject<SoundEvent> GRIMOIRE_LEARN = register("grimoire.learn");
    public static final RegistryObject<SoundEvent> RANK_UP = register("rank.up");
    public static final RegistryObject<SoundEvent> DEMON_SPEAK = register("demon.speak");
    public static final RegistryObject<SoundEvent> AURA_SCALES = register("aura.scales");
    public static final RegistryObject<SoundEvent> MIMIC_CHOMP = register("mimic.chomp");
    public static final RegistryObject<SoundEvent> TELEPORT = register("magic.teleport");
    public static final RegistryObject<SoundEvent> HELLFIRE = register("spell.hellfire");
    public static final RegistryObject<SoundEvent> WIND = register("spell.wind");
    public static final RegistryObject<SoundEvent> LIGHT_RAIN = register("spell.light_rain");
    public static final RegistryObject<SoundEvent> SLASH = register("spell.slash");
    public static final RegistryObject<SoundEvent> CURSE = register("spell.curse");
    public static final RegistryObject<SoundEvent> BARRIER_UP = register("spell.barrier_up");
    public static final RegistryObject<SoundEvent> FLIGHT = register("spell.flight");
    public static final RegistryObject<SoundEvent> UTILITY = register("spell.utility");
    public static final RegistryObject<SoundEvent> DEMON_BOLT = register("demon.bolt");
    public static final RegistryObject<SoundEvent> BOSS_ROAR = register("boss.roar");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(FrierenIds.id(name)));
    }
}
