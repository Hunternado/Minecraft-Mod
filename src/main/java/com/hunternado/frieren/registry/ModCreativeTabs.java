package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * The mod's creative menus, one per part of the mod: magic, gear, building and characters. Every item of the mod
 * appears in exactly one of them (tools/check_tabs.py, run by precheck, reports strays and duplicates).
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FrierenMod.MODID);

    /** Grimoires (and one identified grimoire per known spell), potions, crystals and the journey's paperwork. */
    public static final RegistryObject<CreativeModeTab> MAGIC = TABS.register("frieren_magic", () -> CreativeModeTab.builder()
        .title(Component.translatable(FrierenIds.key("itemGroup", "frieren_magic")))
        .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
        .icon(() -> new ItemStack(ModItems.LEGENDARY_GRIMOIRE.get()))
        .displayItems((parameters, output) -> {
            accept(output, List.of(ModItems.JOURNEY_JOURNAL, ModItems.COMMON_GRIMOIRE, ModItems.UNCOMMON_GRIMOIRE,
                ModItems.RARE_GRIMOIRE, ModItems.ANCIENT_GRIMOIRE, ModItems.LEGENDARY_GRIMOIRE, ModItems.DEMON_GRIMOIRE,
                ModItems.FORBIDDEN_GRIMOIRE, ModItems.MYTHIC_GRIMOIRE, ModItems.SEALED_DEMON_GRIMOIRE, ModItems.SEALED_EXAM_GRIMOIRE,
                ModItems.MANA_POTION, ModItems.HEITER_FLASK, ModItems.SWEET_GRAPES, ModItems.MANA_CRYSTAL,
                ModItems.DEMONIC_MANA_CORE, ModItems.EXAM_SEAL, ModItems.STILLE_CAGE));
            // One identified grimoire per known spell definition (client-side copy, so it reflects the server's datapacks).
            for (var definition : SpellManager.clientOrServerDefinitions()) {
                output.accept(GrimoireItem.forSpell(definition));
            }
        })
        .build());

    /** Staffs, weapons, mage attire and artifacts. */
    public static final RegistryObject<CreativeModeTab> GEAR = TABS.register("frieren_gear", () -> CreativeModeTab.builder()
        .title(Component.translatable(FrierenIds.key("itemGroup", "frieren_gear")))
        .withTabsBefore(MAGIC.getKey())
        .icon(() -> new ItemStack(ModItems.SAGES_STAFF.get()))
        .displayItems((parameters, output) -> accept(output, List.of(ModItems.APPRENTICE_STAFF, ModItems.MAGE_STAFF,
            ModItems.SAGES_STAFF, ModItems.GUILLOTINE_STAFF, ModItems.WARRIORS_AXE, ModItems.HEROS_SWORD,
            ModItems.DEMON_KINGS_BLADE, ModItems.MAGE_HAT, ModItems.MAGE_COAT, ModItems.MAGE_TROUSERS, ModItems.MAGE_BOOTS,
            ModItems.MIRROR_LOTUS_RING, ModItems.CONCEALMENT_CHARM, ModItems.DEMON_HUNTER_INSIGNIA,
            ModItems.SCALE_OF_OBEDIENCE)))
        .build());

    /** The Frieren-style building set, plus the mod's other blocks. */
    public static final RegistryObject<CreativeModeTab> BUILDING = TABS.register("frieren_building", () -> CreativeModeTab.builder()
        .title(Component.translatable(FrierenIds.key("itemGroup", "frieren_building")))
        .withTabsBefore(GEAR.getKey())
        .icon(() -> new ItemStack(ModBuildingBlocks.TAB_ORDER.get(0).get()))
        .displayItems((parameters, output) -> {
            accept(output, ModBuildingBlocks.TAB_ORDER);
            accept(output, List.of(ModItems.MANA_CRYSTAL_ORE, ModItems.DEEPSLATE_MANA_CRYSTAL_ORE, ModItems.MANA_CRYSTAL_BLOCK,
                ModItems.CURSED_GOLD_BLOCK, ModItems.SPELL_RESEARCH_DESK, ModItems.SUMMONING_ALTAR, ModItems.BLUE_MOON_WEED));
        })
        .build());

    /** Everyone you can meet (spawn eggs), the seals that summon bosses, and what they leave behind. */
    public static final RegistryObject<CreativeModeTab> CHARACTERS = TABS.register("frieren_characters", () -> CreativeModeTab.builder()
        .title(Component.translatable(FrierenIds.key("itemGroup", "frieren_characters")))
        .withTabsBefore(BUILDING.getKey())
        .icon(() -> new ItemStack(ModItems.FRIEREN_SPAWN_EGG.get()))
        .displayItems((parameters, output) -> accept(output, List.of(ModItems.FRIEREN_SPAWN_EGG, ModItems.EXAMINER_SPAWN_EGG,
            ModItems.PRIEST_SPAWN_EGG, ModItems.WANDERING_MAGE_SPAWN_EGG, ModItems.STILLE_SPAWN_EGG, ModItems.MIMIC_SPAWN_EGG,
            ModItems.DEMON_SOLDIER_SPAWN_EGG, ModItems.DEMON_MAGE_SPAWN_EGG, ModItems.AURA_THRALL_SPAWN_EGG,
            ModItems.LINIE_SPAWN_EGG, ModItems.DRAHT_SPAWN_EGG, ModItems.LUGNER_SPAWN_EGG,
            ModItems.SIGIL_OF_THE_GUILLOTINE, ModItems.SEAL_OF_CORRUPTION, ModItems.SEAL_OF_THE_GOLDEN_LAND,
            ModItems.SEAL_OF_THE_DEMON_KING, ModItems.MIRROR_SHARD, ModItems.GUILLOTINE_BLADE, ModItems.DEMON_HORN)))
        .build());

    private ModCreativeTabs() {}

    private static void accept(CreativeModeTab.Output output, List<? extends Supplier<? extends Item>> items) {
        for (Supplier<? extends Item> item : items) {
            output.accept(item.get());
        }
    }
}
