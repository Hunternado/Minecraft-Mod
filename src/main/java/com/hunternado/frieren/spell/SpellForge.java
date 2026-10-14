package com.hunternado.frieren.spell;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Serie's spell forge, server side. Everything is re-validated here: Serie within reach, the mage First Class,
 * Serie impressed and Übel beaten, the slot in range, the recipe sanitised, and the price paid. Serie's price is
 * mana crystals ({@link #crystalCost}), plus demonic mana cores for Annihilation; asking again for an occupied slot
 * replaces that spell (its mastery starts over).
 */
public final class SpellForge {
    private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;

    private SpellForge() {}

    /** Four mana crystals per potency level. */
    public static int crystalCost(OriginalSpells.Recipe recipe) {
        return 4 * recipe.sanitized().potency();
    }

    /** Demonic mana cores: four, and only for Annihilation. */
    public static int coreCost(OriginalSpells.Recipe recipe) {
        return recipe.sanitized().potency() >= OriginalSpells.ANNIHILATION ? 4 : 0;
    }

    public static void create(ServerPlayer player, int serieId, int slot, OriginalSpells.Recipe raw) {
        ServerLevel level = player.level();
        Entity entity = level.getEntity(serieId);
        if (!(entity instanceof SerieEntity serie) || !serie.isAlive() || serie.distanceToSqr(player) > MAX_DISTANCE_SQR) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null || data.rank() != MageRank.FIRST_CLASS || !data.serieImpressed() || !data.ubelDefeated()) {
            return;
        }
        if (slot < 0 || slot >= OriginalSpells.MAX_SPELLS) {
            return;
        }
        OriginalSpells.Recipe recipe = raw.sanitized();
        int crystals = crystalCost(recipe);
        int cores = coreCost(recipe);
        if (!player.getAbilities().instabuild) {
            if (count(player, ModItems.MANA_CRYSTAL.get()) < crystals || count(player, ModItems.DEMONIC_MANA_CORE.get()) < cores) {
                Messages.chat(player, Component.translatable("npc.frieren.serie.forge_price", crystals, cores).withStyle(ChatFormatting.RED));
                return;
            }
            take(player, ModItems.MANA_CRYSTAL.get(), crystals);
            take(player, ModItems.DEMONIC_MANA_CORE.get(), cores);
        }

        Identifier id = OriginalSpells.idFor(player.getUUID(), slot);
        if (data.knows(id)) {
            data.forget(id);                                     // a new spell in the old slot: practice starts over
        }
        data.setOriginal(slot, id, recipe);
        SpellDefinition spell = OriginalSpells.build(id, recipe);
        SpellManager.registerServerOriginals(List.of(spell));
        data.learn(id);
        data.markDirty();

        MagicCircleEntity.onGround(level, serie.position().add(0.0D, 0.05D, 0.0D), MagicCircleEntity.STYLE_ARCANE, spell.color(), 2.5F, 60);
        SpellFx.petals(level, player.position(), spell.color(), 2.0F);
        SpellFx.burst(level, player.position().add(0.0D, 1.0D, 0.0D), spell.color(), 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GRIMOIRE_LEARN.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        Messages.chat(player, Component.literal("<").append(serie.getDisplayName()).append("> ")
            .append(Component.translatable("npc.frieren.serie.forge_done", spell.displayName()).withStyle(ChatFormatting.WHITE)));
        Messages.chat(player, Component.translatable("message.frieren.original_learned", spell.displayName()).withStyle(ChatFormatting.GOLD));
        ModTriggers.milestone(player, "original_spell");
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void take(ServerPlayer player, Item item, int amount) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
    }
}
