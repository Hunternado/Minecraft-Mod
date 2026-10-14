package com.hunternado.frieren.exam;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.ExamProgress;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.item.SerieLetterItem;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.registry.ModDataComponents;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Server-side exam state machine. Exams are per-player, so any number of players can sit them at the same time
 * (and friends can help). Every stage runs as a local trial around the examinee (see {@link TrialManager}); the
 * state machine only tracks stage, deadline and completion.
 */
public final class ExamManager {
    private ExamManager() {}

    // ---------------------------------------------------------------------------------------
    // Starting, abandoning, finishing
    // ---------------------------------------------------------------------------------------

    public static boolean start(ServerPlayer player, MagicData data, @Nullable Entity examiner) {
        if (data.exam() != null) {
            Messages.chat(player, Component.translatable("exam.frieren.already_active"));
            return false;
        }
        MageRank next = data.rank().next();
        ExamType type = next != null ? ExamType.forRank(next) : null;
        if (type == null) {
            Messages.chat(player, Component.translatable("exam.frieren.max_rank"));
            return false;
        }
        List<Component> unmet = type.unmetRequirements(data, true);
        if (!unmet.isEmpty()) {
            Messages.chat(player, Component.translatable("exam.frieren.not_eligible", next.displayName()).withStyle(ChatFormatting.RED));
            for (Component line : unmet) {
                Messages.chat(player, Component.literal(" - ").append(line));
            }
            return false;
        }
        data.setExam(new ExamProgress(next, 0L));
        Messages.chat(player, Component.translatable("exam.frieren.started", next.displayName()).withStyle(ChatFormatting.GOLD));
        beginStage(player, data, type, 0, examiner);
        return true;
    }

    /** Admin/testing: starts a specific stage directly, skipping requirements (the exam it belongs to is used). */
    public static boolean startStage(ServerPlayer player, MagicData data, ExamType.Stage stage) {
        for (ExamType type : ExamType.values()) {
            ExamType.Stage[] stages = type.stages();
            for (int i = 0; i < stages.length; i++) {
                if (stages[i] == stage) {
                    TrialManager.end(player.level().getServer(), player.getUUID());
                    data.setExam(new ExamProgress(type.rank(), 0L));
                    beginStage(player, data, type, i, null);
                    return true;
                }
            }
        }
        return false;
    }

    public static void abandon(ServerPlayer player, MagicData data) {
        if (data.exam() != null) {
            fail(player, data, "exam.frieren.fail.abandoned");
        }
    }

    public static void onExamineeDeath(ServerPlayer player, MagicData data) {
        if (data.exam() != null) {
            fail(player, data, "exam.frieren.fail.death");
        }
    }

    private static void beginStage(ServerPlayer player, MagicData data, ExamType type, int index, @Nullable Entity examiner) {
        ExamProgress progress = data.exam();
        if (progress == null) {
            return;
        }
        ServerLevel level = player.level();
        ExamType.Stage stage = type.stage(index);
        progress.setStage(index);
        progress.setDeadline(level.getGameTime() + (long) (stage.timeLimit() * FrierenConfig.examTimeMultiplier()));
        TrialManager.begin(player, stage);
        progress.setObjective(BlockPos.containing(player.position()));
        data.markDirty();
        Messages.chat(player, Component.translatable("exam.frieren.stage", index + 1, type.stages().length, stage.title())
            .withStyle(ChatFormatting.YELLOW));
        Messages.chat(player, stage.instructions().copy().withStyle(ChatFormatting.GRAY));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_CAST.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    /** Called by TrialManager when the current trial's objective is met. */
    public static void completeTrial(ServerPlayer player, MagicData data) {
        completeStage(player, data);
    }

    /** Called by TrialManager when the examinee leaves the arena or its dimension. */
    public static void failTrial(ServerPlayer player, MagicData data, String reasonKey) {
        if (data.exam() != null) {
            fail(player, data, reasonKey);
        } else {
            TrialManager.end(player.level().getServer(), player.getUUID());
        }
    }

    private static void completeStage(ServerPlayer player, MagicData data) {
        FrierenMod.LOGGER.info("[trial] {} completed a stage", player.getName().getString());
        TrialManager.end(player.level().getServer(), player.getUUID());
        ExamProgress progress = data.exam();
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (progress == null || type == null) {
            return;
        }
        int next = progress.stage() + 1;
        if (next < type.stages().length) {
            Messages.chat(player, Component.translatable("exam.frieren.stage_passed").withStyle(ChatFormatting.GREEN));
            beginStage(player, data, type, next, null);
        } else {
            promote(player, data, type);
        }
    }

    public static void promote(ServerPlayer player, MagicData data, ExamType type) {
        TrialManager.end(player.level().getServer(), player.getUUID());
        MageRank rank = type.rank();
        data.setExam(null);
        data.setRank(rank);
        data.markPassed(rank);
        data.setCachedMaxMana(ManaHelper.computeMaxMana(data));
        SpellRarity rewardRarity = switch (rank) {
            case FIFTH_CLASS -> SpellRarity.UNCOMMON;
            case FOURTH_CLASS, THIRD_CLASS -> SpellRarity.RARE;
            case SECOND_CLASS -> SpellRarity.ANCIENT;
            default -> SpellRarity.LEGENDARY;
        };
        ItemStack reward = GrimoireItem.unidentified(rewardRarity);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        if (rank == MageRank.FIRST_CLASS) {
            data.setSeriePrivilegeAvailable(true);
            Messages.chat(player, Component.translatable("exam.frieren.privilege").withStyle(ChatFormatting.LIGHT_PURPLE));
            SerieLetterItem.grantOnce(player, data);
        }
        player.level().getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("exam.frieren.broadcast", player.getDisplayName(), rank.displayName()), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RANK_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ModTriggers.milestone(player, "rank_" + rank.serializedName());
    }

    private static void fail(ServerPlayer player, MagicData data, String reasonKey) {
        FrierenMod.LOGGER.info("[trial] {} failed their exam: {}", player.getName().getString(), reasonKey);
        TrialManager.end(player.level().getServer(), player.getUUID());
        data.setExam(null);
        Messages.chat(player, Component.translatable("exam.frieren.failed", Component.translatable(reasonKey)).withStyle(ChatFormatting.RED));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_FAIL.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // ---------------------------------------------------------------------------------------
    // Hooks
    // ---------------------------------------------------------------------------------------

    public static void tick(ServerPlayer player, MagicData data) {
        ExamProgress progress = data.exam();
        if (progress == null || player.tickCount % 20 != 0) {
            return;
        }
        if (player.level().getGameTime() > progress.deadline()) {
            fail(player, data, "exam.frieren.fail.time");
        }
    }

    /** The examinee logged out: their trial is cleared and the exam clock pauses until they return. */
    public static void onLogout(ServerPlayer player, MagicData data) {
        ExamProgress progress = data.exam();
        TrialManager.end(player.level().getServer(), player.getUUID());
        if (progress != null && progress.paused() <= 0L) {
            progress.setPaused(Math.max(20L, progress.deadline() - player.level().getGameTime()));
            data.markDirty();
        }
    }

    /** The examinee is back: the clock resumes and the current stage's trial restarts where they stand. */
    public static void onLogin(ServerPlayer player, MagicData data) {
        ExamProgress progress = data.exam();
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (progress == null || type == null) {
            return;
        }
        if (progress.paused() > 0L) {
            progress.setDeadline(player.level().getGameTime() + progress.paused());
            progress.setPaused(0L);
        }
        progress.setCounter(0);
        data.markDirty();
        // Give the client a moment to load the world around them before the trial reappears.
        TrialManager.scheduleResume(player.getUUID(), type.stage(progress.stage()));
    }

    /** Called by TrialManager once a resumed trial has been rebuilt. */
    static void onTrialResumed(ServerPlayer player, MagicData data, ExamType.Stage stage) {
        ExamProgress progress = data.exam();
        if (progress != null) {
            progress.setObjective(BlockPos.containing(player.position()));
            data.markDirty();
        }
        Messages.chat(player, Component.translatable("exam.frieren.trial.resumed", stage.title()).withStyle(ChatFormatting.YELLOW));
    }

    /** The genuine chest of a grimoire hunt was opened; credits the examinee it was planted for. */
    public static void onTrueChestOpened(MimicEntity chest, ServerPlayer opener) {
        UUID owner = TrialManager.ownerOf(chest);
        ServerPlayer examinee = owner != null ? opener.level().getServer().getPlayerList().getPlayer(owner) : opener;
        if (examinee == null) {
            return;
        }
        MagicData data = MagicData.get(examinee);
        ExamProgress progress = data != null ? data.exam() : null;
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (type != null && type.stage(progress.stage()) == ExamType.Stage.RETRIEVAL) {
            Messages.chat(examinee, Component.translatable("exam.frieren.trial.chest_found").withStyle(ChatFormatting.GOLD));
            completeStage(examinee, data);
        }
    }

    /** Stille went into the examinee's cage. */
    public static void onStilleCaught(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        ExamProgress progress = data != null ? data.exam() : null;
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (type != null && type.stage(progress.stage()) == ExamType.Stage.STILLE) {
            completeStage(player, data);
        }
    }

    /** Returning exam items to an examiner (Sealed Examination Grimoire, caged Stille). */
    public static void submitItem(ServerPlayer player, MagicData data) {
        ExamProgress progress = data.exam();
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (progress == null || type == null) {
            return;
        }
        ExamType.Stage stage = type.stage(progress.stage());
        if (stage == ExamType.Stage.RETRIEVAL && removeOne(player, ModItems.SEALED_EXAM_GRIMOIRE.get(), false)) {
            completeStage(player, data);
        } else if (stage == ExamType.Stage.STILLE && removeOne(player, ModItems.STILLE_CAGE.get(), true)) {
            completeStage(player, data);
        } else {
            Messages.chat(player, Component.translatable("exam.frieren.nothing_to_submit"));
        }
    }

    /** Serie's interview: detect her during the moment her concealment flickers. */
    public static void onManaDetection(ServerPlayer player, MagicData data, List<LivingEntity> nearby, int detectionPower) {
        ExamProgress progress = data.exam();
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (type == null || type.stage(progress.stage()) != ExamType.Stage.INTERVIEW) {
            return;
        }
        for (LivingEntity entity : nearby) {
            if (entity instanceof SerieEntity serie && serie.distanceToSqr(player) < 16.0D * 16.0D) {
                if (serie.isFlickering() && detectionPower > serie.flickerConcealLevel()) {
                    Messages.chat(player, Component.translatable("exam.frieren.serie_pass").withStyle(ChatFormatting.LIGHT_PURPLE));
                    completeStage(player, data);
                } else {
                    Messages.chat(player, Component.translatable("exam.frieren.serie_fail").withStyle(ChatFormatting.GRAY));
                }
                return;
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /** Removes one matching item; {@code caged} requires a Stille Cage that actually holds a Stille. */
    private static boolean removeOne(ServerPlayer player, Item item, boolean caged) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item) && (!caged || Boolean.TRUE.equals(stack.get(ModDataComponents.CAGED_STILLE.get())))) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
