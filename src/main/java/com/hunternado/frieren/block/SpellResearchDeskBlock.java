package com.hunternado.frieren.block;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.registry.ModDataComponents;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Spell Research Desk.
 * <ul>
 *   <li>Sealed demon grimoire + 4 mana crystals + 40 mana (Third-Class or higher): analyse it into a
 *   readable demon grimoire, as humans once analysed Qual's Zoltraak.</li>
 *   <li>Identified grimoire of a spell you know + a book + 2 mana crystals: transcribe a copy for a friend.</li>
 * </ul>
 */
public class SpellResearchDeskBlock extends Block {
    private static final int ANALYSIS_CRYSTALS = 4;
    private static final float ANALYSIS_MANA = 40.0F;
    private static final int COPY_CRYSTALS = 2;

    public SpellResearchDeskBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(stack.getItem() instanceof GrimoireItem grimoire)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        MagicData data = MagicData.get(serverPlayer);
        if (data == null) {
            return InteractionResult.PASS;
        }
        if (grimoire.isSealed()) {
            analyse(serverPlayer, data, stack, level, pos);
        } else {
            transcribe(serverPlayer, data, stack, level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            Messages.chat(player, Component.translatable("message.frieren.desk_help").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    private static void analyse(ServerPlayer player, MagicData data, ItemStack sealed, Level level, BlockPos pos) {
        if (!data.rank().canLearn(SpellRarity.DEMON)) {
            Messages.chat(player, Component.translatable("message.frieren.desk_rank", MageRank.requiredFor(SpellRarity.DEMON).displayName())
                .withStyle(ChatFormatting.RED));
            return;
        }
        if (countItem(player, ModItems.MANA_CRYSTAL.get()) < ANALYSIS_CRYSTALS || data.mana() < ANALYSIS_MANA) {
            Messages.chat(player, Component.translatable("message.frieren.desk_cost", ANALYSIS_CRYSTALS, (int) ANALYSIS_MANA).withStyle(ChatFormatting.RED));
            return;
        }
        Identifier spell = GrimoireItem.spellOf(sealed);
        ItemStack result;
        if (spell != null && SpellManager.server(spell) != null) {
            result = GrimoireItem.forSpell(SpellManager.server(spell));
        } else {
            SpellDefinition random = SpellManager.randomOfRarity(SpellRarity.DEMON, player.getRandom());
            result = random != null ? GrimoireItem.forSpell(random) : GrimoireItem.unidentified(SpellRarity.DEMON);
        }
        removeItems(player, ModItems.MANA_CRYSTAL.get(), ANALYSIS_CRYSTALS);
        data.trySpendMana(ANALYSIS_MANA);
        if (!player.getAbilities().instabuild) {
            sealed.shrink(1);
        }
        give(player, result);
        Messages.chat(player, Component.translatable("message.frieren.desk_analysed", result.getHoverName()).withStyle(ChatFormatting.DARK_RED));
        level.playSound(null, pos, ModSounds.GRIMOIRE_LEARN.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    private static void transcribe(ServerPlayer player, MagicData data, ItemStack grimoire, Level level, BlockPos pos) {
        Identifier spell = GrimoireItem.spellOf(grimoire);
        if (spell == null || !data.knows(spell)) {
            Messages.chat(player, Component.translatable("message.frieren.desk_unknown_spell").withStyle(ChatFormatting.RED));
            return;
        }
        if (countItem(player, ModItems.MANA_CRYSTAL.get()) < COPY_CRYSTALS || countItem(player, Items.BOOK) < 1) {
            Messages.chat(player, Component.translatable("message.frieren.desk_copy_cost", COPY_CRYSTALS).withStyle(ChatFormatting.RED));
            return;
        }
        removeItems(player, ModItems.MANA_CRYSTAL.get(), COPY_CRYSTALS);
        removeItems(player, Items.BOOK, 1);
        ItemStack copy = grimoire.copyWithCount(1);
        copy.set(ModDataComponents.GRIMOIRE_SPELL.get(), spell);
        give(player, copy);
        Messages.chat(player, Component.translatable("message.frieren.desk_copied", copy.getHoverName()));
        level.playSound(null, pos, ModSounds.UTILITY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    static int countItem(Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static void removeItems(Player player, net.minecraft.world.item.Item item, int amount) {
        if (player.getAbilities().instabuild) {
            return;
        }
        int remaining = amount;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
