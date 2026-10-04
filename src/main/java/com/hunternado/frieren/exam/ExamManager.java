package com.hunternado.frieren.exam;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.combat.SpellDamageSource;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.ExamProgress;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.creature.StilleEntity;
import com.hunternado.frieren.entity.demon.DemonEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.mana.ManaHelper;
import com.hunternado.frieren.registry.ModDataComponents;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.util.Hostility;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Server-side exam state machine. Exams are per-player, so any number of players can sit them at the
 * same time; shared objectives (Spiegel) credit every examinee nearby.
 */
public final class ExamManager {
    public static final TagKey<Structure> EXAM_LIBRARY = TagKey.create(Registries.STRUCTURE, FrierenIds.id("exam_library"));
    public static final TagKey<Structure> EXAM_LABYRINTH = TagKey.create(Registries.STRUCTURE, FrierenIds.id("exam_labyrinth"));
    private static final double SHARED_CREDIT_RADIUS = 48.0D;

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
        progress.setObjective(null);

        switch (stage) {
            case RETRIEVAL -> {
                give(player, ModItems.EXAM_SEAL.get());
                progress.setObjective(level.findNearestMapStructure(EXAM_LIBRARY, player.blockPosition(), 100, false));
            }
            case LABYRINTH -> {
                give(player, ModItems.EXAM_SEAL.get());
                give(player, ModItems.MIRROR_SHARD.get());
                progress.setObjective(level.findNearestMapStructure(EXAM_LABYRINTH, player.blockPosition(), 100, false));
            }
            case STILLE -> {
                give(player, ModItems.STILLE_CAGE.get());
                spawnStille(level, player, examiner != null ? examiner.position() : player.position());
            }
            case INTERVIEW -> ensureSerie(level, examiner != null ? examiner.position() : player.position());
            default -> {
            }
        }
        data.markDirty();
        Messages.chat(player, Component.translatable("exam.frieren.stage", index + 1, type.stages().length, stage.title())
            .withStyle(ChatFormatting.YELLOW));
        Messages.chat(player, stage.instructions().copy().withStyle(ChatFormatting.GRAY));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_CAST.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    private static void completeStage(ServerPlayer player, MagicData data) {
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
        }
        player.level().getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("exam.frieren.broadcast", player.getDisplayName(), rank.displayName()), false);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RANK_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ModTriggers.milestone(player, "rank_" + rank.serializedName());
    }

    private static void fail(ServerPlayer player, MagicData data, String reasonKey) {
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

    public static void onKill(ServerPlayer player, MagicData data, LivingEntity victim, DamageSource source) {
        ExamProgress progress = data.exam();
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (progress == null || type == null || !Hostility.isHostile(victim)) {
            return;
        }
        ExamType.Stage stage = type.stage(progress.stage());
        if (stage == ExamType.Stage.PRACTICAL && source instanceof SpellDamageSource) {
            progress.incrementCounter();
            Messages.actionBar(player, Component.translatable("exam.frieren.progress", progress.counter(), stage.goal()));
            data.markDirty();
            if (progress.counter() >= stage.goal()) {
                completeStage(player, data);
            }
        } else if (stage == ExamType.Stage.SURVIVAL) {
            progress.incrementCounter();
            if (victim instanceof DemonEntity) {
                progress.incrementSecondaryCounter();
            }
            Messages.actionBar(player, Component.translatable("exam.frieren.progress_survival", progress.counter(), stage.goal(),
                progress.secondaryCounter(), ExamType.SURVIVAL_DEMONS));
            data.markDirty();
            if (progress.counter() >= stage.goal() && progress.secondaryCounter() >= ExamType.SURVIVAL_DEMONS) {
                completeStage(player, data);
            }
        }
    }

    /** Spiegel credits every examinee in a labyrinth stage nearby. */
    public static void onSpiegelDefeated(ServerLevel level, Vec3 at) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(at) > SHARED_CREDIT_RADIUS * SHARED_CREDIT_RADIUS) {
                continue;
            }
            MagicData data = MagicData.get(player);
            ExamProgress progress = data != null ? data.exam() : null;
            ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
            if (type != null && type.stage(progress.stage()) == ExamType.Stage.LABYRINTH) {
                completeStage(player, data);
            }
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

    private static void give(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

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

    private static void spawnStille(ServerLevel level, ServerPlayer owner, Vec3 near) {
        StilleEntity stille = ModEntities.STILLE.get().create(level, EntitySpawnReason.EVENT);
        if (stille == null) {
            return;
        }
        stille.snapTo(near.x + 4.0D, near.y + 3.0D, near.z + 4.0D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        stille.setExaminee(owner.getUUID());
        level.addFreshEntity(stille);
    }

    private static void ensureSerie(ServerLevel level, Vec3 near) {
        List<SerieEntity> existing = level.getEntitiesOfClass(SerieEntity.class, new AABB(BlockPos.containing(near)).inflate(16.0D));
        if (!existing.isEmpty()) {
            return;
        }
        SerieEntity serie = ModEntities.SERIE.get().create(level, EntitySpawnReason.EVENT);
        if (serie == null) {
            return;
        }
        serie.snapTo(near.x + 2.0D, near.y, near.z, 0.0F, 0.0F);
        serie.setTemporary(true);
        level.addFreshEntity(serie);
    }
}
