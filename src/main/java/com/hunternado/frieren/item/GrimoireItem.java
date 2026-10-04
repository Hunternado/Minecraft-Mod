package com.hunternado.frieren.item;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.registry.ModDataComponents;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A grimoire teaches one spell. Hold right-click to decipher it (time depends on rarity); the spell is
 * stored in the {@code frieren:grimoire_spell} component. Grimoires without the component are
 * "unidentified" and reveal a random spell of their rarity when first deciphered, so loot tables can simply
 * drop e.g. {@code frieren:rare_grimoire}.
 */
public class GrimoireItem extends Item {
    private final SpellRarity rarity;
    private final boolean sealed;

    public GrimoireItem(Properties properties, SpellRarity rarity, boolean sealed) {
        super(properties);
        this.rarity = rarity;
        this.sealed = sealed;
    }

    public SpellRarity spellRarity() {
        return rarity;
    }

    public boolean isSealed() {
        return sealed;
    }

    public static ItemStack forSpell(SpellDefinition definition) {
        ItemStack stack = new ItemStack(ModItems.grimoireFor(definition.rarity()));
        stack.set(ModDataComponents.GRIMOIRE_SPELL.get(), definition.id());
        return stack;
    }

    public static ItemStack unidentified(SpellRarity rarity) {
        return new ItemStack(ModItems.grimoireFor(rarity));
    }

    public static @Nullable Identifier spellOf(ItemStack stack) {
        return stack.get(ModDataComponents.GRIMOIRE_SPELL.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (sealed) {
            if (!level.isClientSide()) {
                Messages.actionBar(player, Component.translatable("message.frieren.grimoire_sealed"));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return rarity.decipherTicks();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return stack;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return stack;
        }
        Identifier spellId = spellOf(stack);
        if (spellId == null) {
            SpellDefinition random = SpellManager.randomOfRarity(rarity, player.getRandom());
            if (random == null) {
                Messages.chat(player, Component.translatable("message.frieren.grimoire_empty"));
                return stack;
            }
            spellId = random.id();
            stack.set(ModDataComponents.GRIMOIRE_SPELL.get(), spellId);
        }
        SpellDefinition definition = SpellManager.server(spellId);
        if (definition == null) {
            Messages.chat(player, Component.translatable("message.frieren.missing_definition", spellId.toString()));
            return stack;
        }
        if (data.knows(spellId)) {
            Messages.chat(player, Component.translatable("message.frieren.grimoire_known", definition.displayName()));
            return stack;
        }
        if (!data.rank().canLearn(definition.rarity())) {
            Messages.chat(player, Component.translatable("message.frieren.grimoire_rank", definition.displayName(),
                MageRank.requiredFor(definition.rarity()).displayName()).withStyle(ChatFormatting.RED));
            return stack;
        }
        data.learn(spellId);
        Messages.chat(player, Component.translatable("message.frieren.grimoire_learned", definition.displayName()).withStyle(ChatFormatting.AQUA));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GRIMOIRE_LEARN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        QuestManager.onSpellLearned(player, data);
        ModTriggers.milestone(player, "learned_" + definition.rarity().serializedName());
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier spellId = spellOf(stack);
        if (spellId == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.frieren.grimoire_of", Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath()))
            .withStyle(rarity.color());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return rarity == SpellRarity.LEGENDARY || rarity == SpellRarity.FORBIDDEN;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier spellId = spellOf(stack);
        tooltip.accept(rarity.displayName());
        if (sealed) {
            tooltip.accept(Component.translatable("tooltip.frieren.grimoire_sealed").withStyle(ChatFormatting.DARK_RED));
            return;
        }
        if (spellId == null) {
            tooltip.accept(Component.translatable("tooltip.frieren.grimoire_unidentified").withStyle(ChatFormatting.GRAY));
            return;
        }
        SpellDefinition definition = SpellManager.client(spellId);
        if (definition == null) {
            definition = SpellManager.server(spellId);
        }
        if (definition == null) {
            return;
        }
        tooltip.accept(definition.school().displayName());
        tooltip.accept(definition.description().copy().withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.frieren.spell_cost", Math.round(definition.manaCost()),
            String.format("%.1f", definition.cooldown() / 20.0F)).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("tooltip.frieren.requires_rank", definition.requiredRank().displayName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
