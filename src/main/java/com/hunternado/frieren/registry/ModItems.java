package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.item.ArtifactItem;
import com.hunternado.frieren.item.ExamSealItem;
import com.hunternado.frieren.item.FrierenSpawnEggItem;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.item.JourneyJournalItem;
import com.hunternado.frieren.item.ManaPotionItem;
import com.hunternado.frieren.item.ModArmorMaterials;
import com.hunternado.frieren.item.StaffItem;
import com.hunternado.frieren.item.StilleCageItem;
import com.hunternado.frieren.item.SummoningSigilItem;
import com.hunternado.frieren.item.TooltipItem;
import com.hunternado.frieren.magic.SpellRarity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FrierenMod.MODID);

    // ---- Block items -------------------------------------------------------------------------
    public static final RegistryObject<Item> MANA_CRYSTAL_ORE = blockItem("mana_crystal_ore", ModBlocks.MANA_CRYSTAL_ORE);
    public static final RegistryObject<Item> DEEPSLATE_MANA_CRYSTAL_ORE = blockItem("deepslate_mana_crystal_ore", ModBlocks.DEEPSLATE_MANA_CRYSTAL_ORE);
    public static final RegistryObject<Item> MANA_CRYSTAL_BLOCK = blockItem("mana_crystal_block", ModBlocks.MANA_CRYSTAL_BLOCK);
    public static final RegistryObject<Item> SPELL_RESEARCH_DESK = blockItem("spell_research_desk", ModBlocks.SPELL_RESEARCH_DESK);
    public static final RegistryObject<Item> SUMMONING_ALTAR = blockItem("summoning_altar", ModBlocks.SUMMONING_ALTAR);
    public static final RegistryObject<Item> CURSED_GOLD_BLOCK = blockItem("cursed_gold_block", ModBlocks.CURSED_GOLD_BLOCK);
    public static final RegistryObject<Item> BLUE_MOON_WEED = blockItem("blue_moon_weed", ModBlocks.BLUE_MOON_WEED);

    // ---- Grimoires ---------------------------------------------------------------------------
    public static final RegistryObject<Item> COMMON_GRIMOIRE = grimoire("common_grimoire", SpellRarity.COMMON, Rarity.COMMON, false);
    public static final RegistryObject<Item> UNCOMMON_GRIMOIRE = grimoire("uncommon_grimoire", SpellRarity.UNCOMMON, Rarity.UNCOMMON, false);
    public static final RegistryObject<Item> RARE_GRIMOIRE = grimoire("rare_grimoire", SpellRarity.RARE, Rarity.RARE, false);
    public static final RegistryObject<Item> ANCIENT_GRIMOIRE = grimoire("ancient_grimoire", SpellRarity.ANCIENT, Rarity.RARE, false);
    public static final RegistryObject<Item> LEGENDARY_GRIMOIRE = grimoire("legendary_grimoire", SpellRarity.LEGENDARY, Rarity.EPIC, false);
    public static final RegistryObject<Item> DEMON_GRIMOIRE = grimoire("demon_grimoire", SpellRarity.DEMON, Rarity.EPIC, false);
    public static final RegistryObject<Item> FORBIDDEN_GRIMOIRE = grimoire("forbidden_grimoire", SpellRarity.FORBIDDEN, Rarity.EPIC, false);
    /** Dropped by demons; must be analysed at a Spell Research Desk before it can be read. */
    public static final RegistryObject<Item> SEALED_DEMON_GRIMOIRE = grimoire("sealed_demon_grimoire", SpellRarity.DEMON, Rarity.RARE, true);

    // ---- Staffs ------------------------------------------------------------------------------
    public static final RegistryObject<Item> APPRENTICE_STAFF = staff("apprentice_staff", StaffItem.Tier.APPRENTICE, Rarity.COMMON);
    public static final RegistryObject<Item> MAGE_STAFF = staff("mage_staff", StaffItem.Tier.MAGE, Rarity.UNCOMMON);
    public static final RegistryObject<Item> SAGES_STAFF = staff("sages_staff", StaffItem.Tier.SAGE, Rarity.RARE);
    public static final RegistryObject<Item> GUILLOTINE_STAFF = staff("guillotine_staff", StaffItem.Tier.GUILLOTINE, Rarity.EPIC);

    // ---- Mage's Attire -----------------------------------------------------------------------
    public static final RegistryObject<Item> MAGE_HAT = armor("mage_hat", ArmorType.HELMET);
    public static final RegistryObject<Item> MAGE_COAT = armor("mage_coat", ArmorType.CHESTPLATE);
    public static final RegistryObject<Item> MAGE_TROUSERS = armor("mage_trousers", ArmorType.LEGGINGS);
    public static final RegistryObject<Item> MAGE_BOOTS = armor("mage_boots", ArmorType.BOOTS);

    // ---- Materials ---------------------------------------------------------------------------
    public static final RegistryObject<Item> MANA_CRYSTAL = simple("mana_crystal", p -> new TooltipItem(p, "mana_crystal"));
    public static final RegistryObject<Item> DEMON_HORN = simple("demon_horn", p -> new TooltipItem(p, "demon_horn"));
    public static final RegistryObject<Item> DEMONIC_MANA_CORE = simple("demonic_mana_core", p -> new TooltipItem(p.rarity(Rarity.UNCOMMON), "demonic_mana_core"));
    public static final RegistryObject<Item> GUILLOTINE_BLADE = simple("guillotine_blade", p -> new TooltipItem(p.stacksTo(1).rarity(Rarity.EPIC), "guillotine_blade"));

    // ---- Consumables -------------------------------------------------------------------------
    public static final RegistryObject<Item> MANA_POTION = simple("mana_potion", p -> new ManaPotionItem(p.stacksTo(16), 60.0F, false));
    public static final RegistryObject<Item> HEITER_FLASK = simple("heiter_flask", p -> new ManaPotionItem(p.stacksTo(1).rarity(Rarity.UNCOMMON), 150.0F, true));

    // ---- Artifacts ---------------------------------------------------------------------------
    public static final RegistryObject<Item> MIRROR_LOTUS_RING = simple("mirror_lotus_ring", p -> new ArtifactItem(p.stacksTo(1).rarity(Rarity.EPIC), ArtifactItem.Kind.MIRROR_LOTUS_RING));
    public static final RegistryObject<Item> CONCEALMENT_CHARM = simple("concealment_charm", p -> new ArtifactItem(p.stacksTo(1).rarity(Rarity.RARE), ArtifactItem.Kind.CONCEALMENT_CHARM));
    public static final RegistryObject<Item> DEMON_HUNTER_INSIGNIA = simple("demon_hunter_insignia", p -> new ArtifactItem(p.stacksTo(1).rarity(Rarity.RARE), ArtifactItem.Kind.DEMON_HUNTER_INSIGNIA));

    // ---- Exams, quests, summoning ------------------------------------------------------------
    public static final RegistryObject<Item> EXAM_SEAL = simple("exam_seal", p -> new ExamSealItem(p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> STILLE_CAGE = simple("stille_cage", p -> new StilleCageItem(p.stacksTo(1)));
    public static final RegistryObject<Item> SEALED_EXAM_GRIMOIRE = simple("sealed_exam_grimoire", p -> new TooltipItem(p.stacksTo(1).rarity(Rarity.UNCOMMON), "sealed_exam_grimoire"));
    public static final RegistryObject<Item> JOURNEY_JOURNAL = simple("journey_journal", p -> new JourneyJournalItem(p.stacksTo(1)));
    public static final RegistryObject<Item> SIGIL_OF_THE_GUILLOTINE = simple("sigil_of_the_guillotine", p -> new SummoningSigilItem(p.stacksTo(1).rarity(Rarity.EPIC), SummoningSigilItem.Boss.AURA));
    public static final RegistryObject<Item> SEAL_OF_CORRUPTION = simple("seal_of_corruption", p -> new SummoningSigilItem(p.stacksTo(1).rarity(Rarity.RARE), SummoningSigilItem.Boss.QUAL));
    public static final RegistryObject<Item> MIRROR_SHARD = simple("mirror_shard", p -> new SummoningSigilItem(p.stacksTo(16).rarity(Rarity.UNCOMMON), SummoningSigilItem.Boss.SPIEGEL));

    // ---- Spawn eggs (spawn through a supplier so item construction never needs entity types) --
    public static final RegistryObject<Item> DEMON_SOLDIER_SPAWN_EGG = spawnEgg("demon_soldier_spawn_egg", ModEntities.DEMON_SOLDIER);
    public static final RegistryObject<Item> DEMON_MAGE_SPAWN_EGG = spawnEgg("demon_mage_spawn_egg", ModEntities.DEMON_MAGE);
    public static final RegistryObject<Item> AURA_THRALL_SPAWN_EGG = spawnEgg("aura_thrall_spawn_egg", ModEntities.AURA_THRALL);
    public static final RegistryObject<Item> MIMIC_SPAWN_EGG = spawnEgg("mimic_spawn_egg", ModEntities.MIMIC);
    public static final RegistryObject<Item> STILLE_SPAWN_EGG = spawnEgg("stille_spawn_egg", ModEntities.STILLE);
    public static final RegistryObject<Item> WANDERING_MAGE_SPAWN_EGG = spawnEgg("wandering_mage_spawn_egg", ModEntities.WANDERING_MAGE);
    public static final RegistryObject<Item> EXAMINER_SPAWN_EGG = spawnEgg("examiner_spawn_egg", ModEntities.EXAMINER);
    public static final RegistryObject<Item> PRIEST_SPAWN_EGG = spawnEgg("priest_spawn_egg", ModEntities.PRIEST);
    public static final RegistryObject<Item> FRIEREN_SPAWN_EGG = spawnEgg("frieren_spawn_egg", ModEntities.FRIEREN);

    private ModItems() {}

    public static boolean isMageAttire(ItemStack stack) {
        return stack.is(MAGE_HAT.get()) || stack.is(MAGE_COAT.get()) || stack.is(MAGE_TROUSERS.get()) || stack.is(MAGE_BOOTS.get());
    }

    public static Item grimoireFor(SpellRarity rarity) {
        return switch (rarity) {
            case COMMON -> COMMON_GRIMOIRE.get();
            case UNCOMMON -> UNCOMMON_GRIMOIRE.get();
            case RARE -> RARE_GRIMOIRE.get();
            case ANCIENT -> ANCIENT_GRIMOIRE.get();
            case LEGENDARY -> LEGENDARY_GRIMOIRE.get();
            case DEMON -> DEMON_GRIMOIRE.get();
            case FORBIDDEN -> FORBIDDEN_GRIMOIRE.get();
        };
    }

    private static RegistryObject<Item> simple(String name, Function<Item.Properties, Item> factory) {
        return ITEMS.register(name, () -> factory.apply(new Item.Properties().setId(ITEMS.key(name))));
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().setId(ITEMS.key(name)).useBlockDescriptionPrefix()));
    }

    private static RegistryObject<Item> grimoire(String name, SpellRarity rarity, Rarity itemRarity, boolean sealed) {
        return simple(name, p -> new GrimoireItem(p.stacksTo(1).rarity(itemRarity), rarity, sealed));
    }

    private static RegistryObject<Item> staff(String name, StaffItem.Tier tier, Rarity rarity) {
        return simple(name, p -> new StaffItem(p.stacksTo(1).rarity(rarity), tier));
    }

    private static RegistryObject<Item> armor(String name, ArmorType type) {
        return simple(name, p -> new TooltipItem(p.humanoidArmor(ModArmorMaterials.MAGE_ATTIRE, type), "mage_attire"));
    }

    private static <T extends Mob> RegistryObject<Item> spawnEgg(String name, Supplier<EntityType<T>> type) {
        return simple(name, p -> new FrierenSpawnEggItem(p, type::get));
    }
}
