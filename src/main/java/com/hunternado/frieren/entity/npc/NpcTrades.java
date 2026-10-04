package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Trade tables for NPC merchants. Offers are rebuilt deterministically from the NPC's UUID. */
public final class NpcTrades {
    /** A single trade. {@code costB} may be empty. */
    public record Offer(ItemStack costA, ItemStack costB, ItemStack result) {}

    private NpcTrades() {}

    private static Offer offer(ItemStack a, ItemStack b, ItemStack result) {
        return new Offer(a, b, result);
    }

    private static ItemStack emeralds(int count) {
        return new ItemStack(Items.EMERALD, count);
    }

    private static ItemStack crystals(int count) {
        return new ItemStack(ModItems.MANA_CRYSTAL.get(), count);
    }

    private static ItemStack grimoireOf(String spellPath, SpellRarity fallback) {
        SpellDefinition definition = SpellManager.server(FrierenIds.id(spellPath));
        return definition != null ? GrimoireItem.forSpell(definition) : GrimoireItem.unidentified(fallback);
    }

    private static List<Offer> pick(List<Supplier<Offer>> pool, int count, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<Supplier<Offer>> copy = new ArrayList<>(pool);
        List<Offer> result = new ArrayList<>();
        while (!copy.isEmpty() && result.size() < count) {
            result.add(copy.remove(random.nextInt(copy.size())).get());
        }
        return result;
    }

    public static List<Offer> wanderingMage(long seed) {
        List<Supplier<Offer>> pool = List.of(
            () -> offer(emeralds(8), ItemStack.EMPTY, GrimoireItem.unidentified(SpellRarity.COMMON)),
            () -> offer(emeralds(16), crystals(2), GrimoireItem.unidentified(SpellRarity.UNCOMMON)),
            () -> offer(emeralds(32), crystals(6), GrimoireItem.unidentified(SpellRarity.RARE)),
            () -> offer(emeralds(4), ItemStack.EMPTY, new ItemStack(ModItems.MANA_POTION.get(), 2)),
            () -> offer(emeralds(12), ItemStack.EMPTY, new ItemStack(ModItems.APPRENTICE_STAFF.get())),
            () -> offer(emeralds(24), crystals(12), new ItemStack(ModItems.MAGE_STAFF.get())),
            () -> offer(emeralds(10), ItemStack.EMPTY, new ItemStack(ModItems.MAGE_HAT.get())),
            () -> offer(emeralds(14), ItemStack.EMPTY, new ItemStack(ModItems.MAGE_COAT.get())),
            () -> offer(crystals(4), ItemStack.EMPTY, emeralds(6)),
            () -> offer(emeralds(20), ItemStack.EMPTY, new ItemStack(ModItems.SPELL_RESEARCH_DESK.get()))
        );
        return pick(pool, 6, seed);
    }

    public static List<Offer> priest(long seed) {
        List<Supplier<Offer>> pool = List.of(
            () -> offer(emeralds(10), ItemStack.EMPTY, grimoireOf("goddess_healing", SpellRarity.UNCOMMON)),
            () -> offer(emeralds(10), ItemStack.EMPTY, grimoireOf("curse_purification", SpellRarity.UNCOMMON)),
            () -> offer(emeralds(3), ItemStack.EMPTY, new ItemStack(ModItems.MANA_POTION.get())),
            () -> offer(new ItemStack(ModItems.DEMON_HORN.get(), 4), ItemStack.EMPTY, emeralds(6)),
            () -> offer(new ItemStack(Items.GOLD_INGOT, 6), ItemStack.EMPTY, new ItemStack(ModItems.HEITER_FLASK.get())),
            () -> offer(emeralds(18), crystals(4), new ItemStack(ModItems.CONCEALMENT_CHARM.get()))
        );
        return pick(pool, 5, seed);
    }

    /** Frieren collects (and sells) the everyday spells nobody else bothers with. */
    public static List<Offer> frieren(long seed) {
        List<Supplier<Offer>> pool = List.of(
            () -> offer(emeralds(6), ItemStack.EMPTY, grimoireOf("flower_field", SpellRarity.COMMON)),
            () -> offer(emeralds(6), ItemStack.EMPTY, grimoireOf("mimic_detection", SpellRarity.COMMON)),
            () -> offer(emeralds(6), ItemStack.EMPTY, grimoireOf("rust_removal", SpellRarity.COMMON)),
            () -> offer(emeralds(8), ItemStack.EMPTY, grimoireOf("clothes_cleaning", SpellRarity.UNCOMMON)),
            () -> offer(emeralds(5), ItemStack.EMPTY, grimoireOf("light_orb", SpellRarity.COMMON)),
            () -> offer(emeralds(5), ItemStack.EMPTY, grimoireOf("gathering_magic", SpellRarity.COMMON)),
            () -> offer(emeralds(9), ItemStack.EMPTY, grimoireOf("seeking_magic", SpellRarity.UNCOMMON)),
            () -> offer(new ItemStack(ModItems.BLUE_MOON_WEED.get(), 1), ItemStack.EMPTY, grimoireOf("flight_magic", SpellRarity.UNCOMMON)),
            () -> offer(GrimoireItem.unidentified(SpellRarity.COMMON), ItemStack.EMPTY, emeralds(3))
        );
        return pick(pool, 6, seed);
    }
}
