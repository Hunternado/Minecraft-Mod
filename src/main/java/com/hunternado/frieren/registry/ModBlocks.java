package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.block.ManaLightBlock;
import com.hunternado.frieren.block.SpellResearchDeskBlock;
import com.hunternado.frieren.block.SummoningAltarBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FrierenMod.MODID);

    public static final RegistryObject<Block> MANA_CRYSTAL_ORE = BLOCKS.register("mana_crystal_ore", () ->
        new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("mana_crystal_ore"))
            .mapColor(MapColor.STONE)
            .strength(3.0F, 3.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 3)
            .sound(SoundType.STONE)));

    public static final RegistryObject<Block> DEEPSLATE_MANA_CRYSTAL_ORE = BLOCKS.register("deepslate_mana_crystal_ore", () ->
        new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("deepslate_mana_crystal_ore"))
            .mapColor(MapColor.DEEPSLATE)
            .strength(4.5F, 3.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 3)
            .sound(SoundType.DEEPSLATE)));

    public static final RegistryObject<Block> MANA_CRYSTAL_BLOCK = BLOCKS.register("mana_crystal_block", () ->
        new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("mana_crystal_block"))
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .strength(3.0F, 6.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 9)
            .sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> SPELL_RESEARCH_DESK = BLOCKS.register("spell_research_desk", () ->
        new SpellResearchDeskBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("spell_research_desk"))
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD)
            .noOcclusion()));

    public static final RegistryObject<Block> SUMMONING_ALTAR = BLOCKS.register("summoning_altar", () ->
        new SummoningAltarBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("summoning_altar"))
            .mapColor(MapColor.COLOR_BLACK)
            .strength(50.0F, 1200.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 5)
            .sound(SoundType.DEEPSLATE_BRICKS)));

    public static final RegistryObject<Block> CURSED_GOLD_BLOCK = BLOCKS.register("cursed_gold_block", () ->
        new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("cursed_gold_block"))
            .mapColor(MapColor.GOLD)
            .strength(2.0F, 6.0F)
            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> BLUE_MOON_WEED = BLOCKS.register("blue_moon_weed", () ->
        new FlowerBlock(MobEffects.REGENERATION, 8.0F, BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("blue_moon_weed"))
            .mapColor(MapColor.COLOR_BLUE)
            .noCollision()
            .instabreak()
            .lightLevel(state -> 4)
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)));

    /** Placed by the Light Orb spell; removes itself after a while. Has no item. */
    public static final RegistryObject<Block> MANA_LIGHT = BLOCKS.register("mana_light", () ->
        new ManaLightBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("mana_light"))
            .mapColor(MapColor.NONE)
            .noCollision()
            .instabreak()
            .noOcclusion()
            .lightLevel(state -> 15)
            .replaceable()
            .sound(SoundType.AMETHYST_CLUSTER)
            .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {}
}
