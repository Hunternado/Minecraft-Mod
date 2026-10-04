package com.hunternado.frieren.command;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.data.SpellProgress;
import com.hunternado.frieren.exam.ExamManager;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.quest.Quest;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.spell.SpellCaster;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Map;

/**
 * {@code /frieren} administration and debugging commands. Everything requires game-master permission
 * (level 2).
 */
public final class FrierenCommand {
    private static final SuggestionProvider<CommandSourceStack> SPELLS = (ctx, builder) ->
        SharedSuggestionProvider.suggestResource(SpellManager.serverDefinitions().stream().map(SpellDefinition::id), builder);
    private static final SuggestionProvider<CommandSourceStack> RANKS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(Arrays.stream(MageRank.values()).map(MageRank::serializedName), builder);
    private static final SuggestionProvider<CommandSourceStack> QUESTS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(Arrays.stream(Quest.values()).map(Quest::id), builder);

    private static final SimpleCommandExceptionType NO_DATA = new SimpleCommandExceptionType(Component.translatable("commands.frieren.no_data"));

    private FrierenCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("frieren")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("mana")
                .then(Commands.literal("get")
                    .executes(ctx -> manaGet(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> manaGet(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("fill")
                    .executes(ctx -> manaFill(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> manaFill(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
                            .executes(ctx -> manaSet(ctx, EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "amount"))))))
                .then(Commands.literal("growth")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10000))
                            .executes(ctx -> manaGrowth(ctx, EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount")))))))
            .then(Commands.literal("spell")
                .then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("spell", IdentifierArgument.id()).suggests(SPELLS)
                            .executes(ctx -> spellGive(ctx, EntityArgument.getPlayer(ctx, "player"), IdentifierArgument.getId(ctx, "spell"))))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("spell", IdentifierArgument.id()).suggests(SPELLS)
                            .executes(ctx -> spellRemove(ctx, EntityArgument.getPlayer(ctx, "player"), IdentifierArgument.getId(ctx, "spell"))))))
                .then(Commands.literal("learnall")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> spellLearnAll(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("list")
                    .executes(ctx -> spellList(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> spellList(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("mastery")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("spell", IdentifierArgument.id()).suggests(SPELLS)
                            .then(Commands.argument("value", IntegerArgumentType.integer(0, SpellProgress.MAX_MASTERY))
                                .executes(ctx -> spellMastery(ctx, EntityArgument.getPlayer(ctx, "player"), IdentifierArgument.getId(ctx, "spell"),
                                    IntegerArgumentType.getInteger(ctx, "value"))))))))
            .then(Commands.literal("rank")
                .then(Commands.literal("get")
                    .executes(ctx -> rankGet(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> rankGet(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rank", StringArgumentType.word()).suggests(RANKS)
                            .executes(ctx -> rankSet(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "rank")))))))
            .then(Commands.literal("exam")
                .then(Commands.literal("start")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> examStart(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("abandon")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> examAbandon(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("pass")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> examPass(ctx, EntityArgument.getPlayer(ctx, "player"))))))
            .then(Commands.literal("quest")
                .then(Commands.literal("complete")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUESTS)
                            .executes(ctx -> questComplete(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "quest"))))))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("quest", StringArgumentType.word()).suggests(QUESTS)
                            .executes(ctx -> questReset(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "quest")))))))
            .then(Commands.literal("cooldowns")
                .executes(ctx -> cooldownReset(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> cooldownReset(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> resetPlayer(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("debug")
                .executes(ctx -> debug(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> debug(ctx, EntityArgument.getPlayer(ctx, "player"))))));
    }

    // ---------------------------------------------------------------------------------------

    private static MagicData data(ServerPlayer player) throws CommandSyntaxException {
        MagicData data = MagicData.get(player);
        if (data == null) {
            throw NO_DATA.create();
        }
        return data;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    private static int manaGet(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        return ok(ctx, Component.translatable("commands.frieren.mana.get", player.getDisplayName(),
            String.format("%.1f", data.mana()), data.maxMana(), data.manaGrowth(), data.bonusGrowth()));
    }

    private static int manaFill(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        data.setMana(data.maxMana());
        return ok(ctx, Component.translatable("commands.frieren.mana.filled", player.getDisplayName()));
    }

    private static int manaSet(CommandContext<CommandSourceStack> ctx, ServerPlayer player, float amount) throws CommandSyntaxException {
        MagicData data = data(player);
        data.setMana(amount);
        return ok(ctx, Component.translatable("commands.frieren.mana.set", player.getDisplayName(), String.format("%.1f", data.mana())));
    }

    private static int manaGrowth(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int amount) throws CommandSyntaxException {
        MagicData data = data(player);
        data.setManaGrowth(amount);
        data.setCachedMaxMana(ManaHelper.computeMaxMana(data));
        return ok(ctx, Component.translatable("commands.frieren.mana.growth", player.getDisplayName(), amount, data.maxMana()));
    }

    private static int spellGive(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Identifier spell) throws CommandSyntaxException {
        if (SpellManager.server(spell) == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.spell.unknown", spell.toString()));
            return 0;
        }
        MagicData data = data(player);
        if (!data.learn(spell)) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.spell.already", player.getDisplayName(), spell.toString()));
            return 0;
        }
        QuestManager.onSpellLearned(player, data);
        return ok(ctx, Component.translatable("commands.frieren.spell.given", spell.toString(), player.getDisplayName()));
    }

    private static int spellRemove(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Identifier spell) throws CommandSyntaxException {
        MagicData data = data(player);
        if (!data.forget(spell)) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.spell.not_known", player.getDisplayName(), spell.toString()));
            return 0;
        }
        SpellCaster.deactivateAll(player, data);
        return ok(ctx, Component.translatable("commands.frieren.spell.removed", spell.toString(), player.getDisplayName()));
    }

    private static int spellLearnAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        int learned = 0;
        for (SpellDefinition definition : SpellManager.serverDefinitions()) {
            if (data.learn(definition.id())) {
                learned++;
            }
        }
        QuestManager.onSpellLearned(player, data);
        return ok(ctx, Component.translatable("commands.frieren.spell.learnall", learned, player.getDisplayName()));
    }

    private static int spellList(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.frieren.spell.list", player.getDisplayName(), data.knownCount()), false);
        for (Map.Entry<Identifier, SpellProgress> entry : data.knownSpells().entrySet()) {
            ctx.getSource().sendSuccess(() -> Component.literal(" - " + entry.getKey() + " (mastery " + entry.getValue().mastery()
                + ", casts " + entry.getValue().casts() + ")").withStyle(ChatFormatting.GRAY), false);
        }
        return data.knownCount();
    }

    private static int spellMastery(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Identifier spell, int value) throws CommandSyntaxException {
        MagicData data = data(player);
        SpellProgress progress = data.progress(spell);
        if (progress == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.spell.not_known", player.getDisplayName(), spell.toString()));
            return 0;
        }
        progress.setMastery(value);
        data.markDirty();
        return ok(ctx, Component.translatable("commands.frieren.spell.mastery", spell.toString(), value, player.getDisplayName()));
    }

    private static int rankGet(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        return ok(ctx, Component.translatable("commands.frieren.rank.get", player.getDisplayName(), data.rank().displayName()));
    }

    private static int rankSet(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String rankName) throws CommandSyntaxException {
        MageRank rank = MageRank.byName(rankName);
        if (!rank.serializedName().equals(rankName)) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.rank.unknown", rankName));
            return 0;
        }
        MagicData data = data(player);
        data.setRank(rank);
        data.setCachedMaxMana(ManaHelper.computeMaxMana(data));
        return ok(ctx, Component.translatable("commands.frieren.rank.set", player.getDisplayName(), rank.displayName()));
    }

    private static int examStart(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        return ExamManager.start(player, data(player), null) ? 1 : 0;
    }

    private static int examAbandon(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        ExamManager.abandon(player, data(player));
        return 1;
    }

    private static int examPass(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        MageRank next = data.exam() != null ? data.exam().target() : data.rank().next();
        ExamType type = next != null ? ExamType.forRank(next) : null;
        if (type == null) {
            ctx.getSource().sendFailure(Component.translatable("exam.frieren.max_rank"));
            return 0;
        }
        ExamManager.promote(player, data, type);
        return ok(ctx, Component.translatable("commands.frieren.exam.passed", player.getDisplayName(), type.rank().displayName()));
    }

    private static int questComplete(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id) throws CommandSyntaxException {
        Quest quest = Quest.byId(id);
        if (quest == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.quest.unknown", id));
            return 0;
        }
        MagicData data = data(player);
        if (data.questCompleted(quest.id())) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.quest.already", id));
            return 0;
        }
        QuestManager.complete(player, data, quest);
        return 1;
    }

    private static int questReset(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String id) throws CommandSyntaxException {
        Quest quest = Quest.byId(id);
        if (quest == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.frieren.quest.unknown", id));
            return 0;
        }
        data(player).resetQuest(quest.id());
        return ok(ctx, Component.translatable("commands.frieren.quest.reset", id, player.getDisplayName()));
    }

    private static int cooldownReset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        data(player).clearCooldowns();
        return ok(ctx, Component.translatable("commands.frieren.cooldowns", player.getDisplayName()));
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        SpellCaster.deactivateAll(player, data);
        MagicData fresh = new MagicData();
        fresh.setStarterGiven(true);
        data.load(fresh.save());
        data.setCachedMaxMana(ManaHelper.computeMaxMana(data));
        data.setMana(data.maxMana());
        return ok(ctx, Component.translatable("commands.frieren.reset", player.getDisplayName()));
    }

    private static int debug(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        MagicData data = data(player);
        String exam = data.exam() == null ? "none"
            : data.exam().target().serializedName() + " stage " + data.exam().stage() + " (" + data.exam().counter() + "/" + data.exam().secondaryCounter() + ")";
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
            "[Frieren] %s rank=%s mana=%.1f/%d growth=%d+%d xp=%.1f spells=%d conceal=%s(%.1f%%) detect=%.1f exam=%s flight=%s barrier=%s quests=%s",
            player.getName().getString(), data.rank().serializedName(), data.mana(), data.maxMana(), data.manaGrowth(), data.bonusGrowth(),
            data.trainingXp(), data.knownCount(), data.concealing(), data.concealMastery(), data.detectionMastery(), exam,
            data.flightActive(), data.barrierActive(), data.completedQuests())).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }
}
