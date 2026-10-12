package com.hunternado.frieren.exam;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.data.ExamProgress;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.boss.MirrorFrierenEntity;
import com.hunternado.frieren.entity.boss.SpiegelEntity;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.entity.creature.StilleEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.entity.trial.ExamConstructEntity;
import com.hunternado.frieren.item.StilleCageItem;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.registry.ModItems;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Ground;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Exam trials: every exam stage plays out on the spot, inside a magic circle around the examinee. Nothing is
 * searched for and nothing depends on world generation. Trial entities carry two tags ({@link #TAG} and the
 * examinee's owner tag) and live only as long as their trial: they are discarded when the stage ends, the examinee
 * fails, dies, leaves the arena or logs out, and any copy loaded from disk without a running trial (after a crash
 * or restart) is refused on load. Lost entities that did not die (unloaded, removed by another mod) are replaced,
 * so a stage can never soft-lock. Trial state is in memory only; logging back in restarts the current stage.
 */
public final class TrialManager {
    public static final String TAG = "frieren_trial";
    private static final String OWNER_PREFIX = "frieren_trial.";
    public static final float ARENA_RADIUS = 12.0F;
    private static final double WARN_DISTANCE = 26.0D;
    private static final double LEAVE_DISTANCE = 40.0D;
    private static final int ARENA_COLOR = 0x9FD8FF;
    private static final int ARENA_LIFE = 20 * 60 * 30;
    private static final int SURVIVAL_WAVES = 3;
    private static final int CONSTRUCTS = 6;
    private static final int MIMICS = 4;

    private static final Map<UUID, Trial> ACTIVE = new HashMap<>();
    /** Examinees who just logged in, with the stage to rebuild and the ticks left before doing so. */
    private static final Map<UUID, Map.Entry<ExamType.Stage, Integer>> PENDING = new HashMap<>();
    private static final int RESUME_DELAY = 60;

    private TrialManager() {}

    /** One running trial. Tracked entities must be defeated (or, for chests, opened); props are scenery. */
    static final class Trial {
        final UUID examinee;
        final ResourceKey<Level> dimension;
        final Vec3 center;
        final float facing;
        final ExamType.Stage stage;
        final Map<UUID, EntityType<?>> tracked = new LinkedHashMap<>();
        final Set<UUID> defeated = new HashSet<>();
        final Set<UUID> props = new HashSet<>();
        final Map<UUID, Integer> missingTicks = new HashMap<>();
        int wave;
        int nextWaveIn = -1;
        int lastReported = -1;
        boolean warned;
        boolean finished;

        Trial(UUID examinee, ResourceKey<Level> dimension, Vec3 center, float facing, ExamType.Stage stage) {
            this.examinee = examinee;
            this.dimension = dimension;
            this.center = center;
            this.facing = facing;
            this.stage = stage;
        }

        int remaining() {
            int alive = 0;
            for (UUID id : tracked.keySet()) {
                if (!defeated.contains(id)) {
                    alive++;
                }
            }
            return alive;
        }
    }

    public static String ownerTag(UUID examinee) {
        return OWNER_PREFIX + examinee;
    }

    public static @Nullable UUID ownerOf(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(OWNER_PREFIX)) {
                try {
                    return UUID.fromString(tag.substring(OWNER_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean hasTrial(UUID examinee) {
        return ACTIVE.containsKey(examinee);
    }

    public static @Nullable Vec3 center(UUID examinee) {
        Trial trial = ACTIVE.get(examinee);
        return trial != null ? trial.center : null;
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------

    /** Starts (or restarts) the trial for {@code stage} around the examinee. */
    public static void begin(ServerPlayer player, ExamType.Stage stage) {
        end(player.level().getServer(), player.getUUID());
        ServerLevel level = player.level();
        Vec3 center = player.position();
        Trial trial = new Trial(player.getUUID(), level.dimension(), center, player.getYRot(), stage);
        ACTIVE.put(player.getUUID(), trial);

        MagicCircleEntity arena = MagicCircleEntity.onGround(level, center, MagicCircleEntity.STYLE_ARCANE, ARENA_COLOR, ARENA_RADIUS, ARENA_LIFE);
        if (arena != null) {
            trial.props.add(arena.getUUID());
        }
        SpellFx.ring(level, center.add(0.0D, 0.2D, 0.0D), ARENA_COLOR, ARENA_RADIUS);

        FrierenMod.LOGGER.info("[trial] {} begins {} at {}", player.getName().getString(), stage.id(), BlockPos.containing(center));
        switch (stage) {
            case PRACTICAL -> {
                for (int i = 0; i < CONSTRUCTS; i++) {
                    spawn(trial, level, player, ModEntities.EXAM_CONSTRUCT.get(), true);
                }
            }
            case SURVIVAL -> {
                trial.wave = 0;
                trial.nextWaveIn = 2;
            }
            case RETRIEVAL -> {
                spawn(trial, level, player, ModEntities.MIMIC.get(), true);
                for (int i = 0; i < MIMICS; i++) {
                    spawn(trial, level, player, ModEntities.MIMIC.get(), false);
                }
            }
            case LABYRINTH -> spawn(trial, level, player, ModEntities.SPIEGEL.get(), true);
            case STILLE -> {
                if (!hasEmptyCage(player)) {
                    give(player, new ItemStack(ModItems.STILLE_CAGE.get()));
                }
                spawn(trial, level, player, ModEntities.STILLE.get(), true);
            }
            case REPLICA -> spawn(trial, level, player, ModEntities.MIRROR_FRIEREN.get(), true);
            case INTERVIEW -> spawn(trial, level, player, ModEntities.SERIE.get(), true);
        }
    }

    /** Ends a trial (pass, fail or logout) and removes everything it spawned. */
    public static void end(@Nullable MinecraftServer server, UUID examinee) {
        PENDING.remove(examinee);
        Trial trial = ACTIVE.remove(examinee);
        if (trial == null || server == null) {
            return;
        }
        trial.finished = true;
        FrierenMod.LOGGER.info("[trial] {} trial for {} ended ({} tracked, {} defeated)", trial.stage.id(), examinee,
            trial.tracked.size(), trial.defeated.size());
        ServerLevel level = server.getLevel(trial.dimension);
        if (level == null) {
            return;
        }
        for (UUID id : trial.tracked.keySet()) {
            discard(level, id);
        }
        for (UUID id : trial.props) {
            discard(level, id);
        }
        String owner = ownerTag(examinee);
        AABB area = new AABB(BlockPos.containing(trial.center)).inflate(LEAVE_DISTANCE + 24.0D);
        for (Entity entity : level.getEntities((Entity) null, area, e -> e.entityTags().contains(owner))) {
            entity.discard();
        }
    }

    public static void endAll(MinecraftServer server) {
        for (UUID id : List.copyOf(ACTIVE.keySet())) {
            end(server, id);
        }
        PENDING.clear();
    }

    private static void discard(ServerLevel level, UUID id) {
        Entity entity = level.getEntity(id);
        if (entity != null) {
            entity.discard();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Hooks
    // ---------------------------------------------------------------------------------------

    /** Any living entity died: trial entities count as defeated. */
    public static void onDeath(LivingEntity entity) {
        if (!entity.entityTags().contains(TAG)) {
            return;
        }
        UUID owner = ownerOf(entity);
        Trial trial = owner != null ? ACTIVE.get(owner) : null;
        if (trial != null && trial.tracked.containsKey(entity.getUUID())) {
            trial.defeated.add(entity.getUUID());
        }
    }

    /** Refuses trial entities loaded from disk that no running trial owns (crash, restart, abandoned chunk). */
    public static boolean shouldRefuseLoaded(Entity entity) {
        if (!entity.entityTags().contains(TAG)) {
            return false;
        }
        UUID owner = ownerOf(entity);
        Trial trial = owner != null ? ACTIVE.get(owner) : null;
        return trial == null || !(trial.tracked.containsKey(entity.getUUID()) || trial.props.contains(entity.getUUID()));
    }

    public static void scheduleResume(UUID examinee, ExamType.Stage stage) {
        PENDING.put(examinee, Map.entry(stage, RESUME_DELAY));
    }

    /** Runs every server tick; does its work twice a second. */
    public static void tick(MinecraftServer server) {
        if ((ACTIVE.isEmpty() && PENDING.isEmpty()) || server.getTickCount() % 10 != 0) {
            return;
        }
        for (UUID id : List.copyOf(PENDING.keySet())) {
            Map.Entry<ExamType.Stage, Integer> pending = PENDING.get(id);
            int left = pending.getValue() - 10;
            if (left > 0) {
                PENDING.put(id, Map.entry(pending.getKey(), left));
                continue;
            }
            PENDING.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            MagicData data = player != null ? MagicData.get(player) : null;
            ExamProgress progress = data != null ? data.exam() : null;
            ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
            if (type != null && type.stage(progress.stage()) == pending.getKey() && !ACTIVE.containsKey(id) && player.isAlive()) {
                begin(player, pending.getKey());
                ExamManager.onTrialResumed(player, data, pending.getKey());
            }
        }
        for (Trial trial : List.copyOf(ACTIVE.values())) {
            if (!trial.finished) {
                tickTrial(server, trial);
            }
        }
    }

    private static void tickTrial(MinecraftServer server, Trial trial) {
        ServerPlayer player = server.getPlayerList().getPlayer(trial.examinee);
        MagicData data = player != null ? MagicData.get(player) : null;
        ExamProgress progress = data != null ? data.exam() : null;
        ExamType type = progress != null ? ExamType.forRank(progress.target()) : null;
        if (player == null || data == null || type == null || type.stage(progress.stage()) != trial.stage) {
            end(server, trial.examinee);
            return;
        }
        ServerLevel level = player.level();
        if (level.dimension() != trial.dimension) {
            ExamManager.failTrial(player, data, "exam.frieren.fail.left_arena");
            return;
        }
        double dx = player.getX() - trial.center.x;
        double dz = player.getZ() - trial.center.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > LEAVE_DISTANCE) {
            ExamManager.failTrial(player, data, "exam.frieren.fail.left_arena");
            return;
        }
        if (distance > WARN_DISTANCE && !trial.warned) {
            trial.warned = true;
            Messages.actionBar(player, Component.translatable("exam.frieren.trial.leaving").withStyle(ChatFormatting.RED));
        } else if (distance < WARN_DISTANCE - 4.0D) {
            trial.warned = false;
        }

        replaceMissing(trial, level, player);

        switch (trial.stage) {
            case PRACTICAL -> {
                int remaining = trial.remaining();
                report(trial, player, remaining, Component.translatable("exam.frieren.trial.constructs", CONSTRUCTS - remaining, CONSTRUCTS));
                if (remaining == 0) {
                    ExamManager.completeTrial(player, data);
                }
            }
            case SURVIVAL -> tickSurvival(trial, level, player, data);
            case LABYRINTH, REPLICA -> {
                if (trial.remaining() == 0) {
                    ExamManager.completeTrial(player, data);
                }
            }
            default -> {
                // RETRIEVAL, STILLE and INTERVIEW complete through their own hooks (chest, cage, detection).
            }
        }
    }

    private static void tickSurvival(Trial trial, ServerLevel level, ServerPlayer player, MagicData data) {
        if (trial.nextWaveIn > 0) {
            trial.nextWaveIn--;
            return;
        }
        if (trial.nextWaveIn == 0) {
            trial.nextWaveIn = -1;
            trial.wave++;
            spawnWave(trial, level, player);
            Messages.chat(player, Component.translatable("exam.frieren.trial.wave", trial.wave, SURVIVAL_WAVES).withStyle(ChatFormatting.YELLOW));
            return;
        }
        int remaining = trial.remaining();
        report(trial, player, trial.wave * 100 + remaining,
            Component.translatable("exam.frieren.trial.wave_progress", trial.wave, SURVIVAL_WAVES, remaining));
        if (remaining > 0) {
            return;
        }
        if (trial.wave >= SURVIVAL_WAVES) {
            ExamManager.completeTrial(player, data);
        } else {
            trial.tracked.clear();
            trial.defeated.clear();
            trial.nextWaveIn = 6;
            Messages.actionBar(player, Component.translatable("exam.frieren.trial.wave_cleared").withStyle(ChatFormatting.GREEN));
        }
    }

    private static void spawnWave(Trial trial, ServerLevel level, ServerPlayer player) {
        int helpers = Math.min(3, level.getEntitiesOfClass(ServerPlayer.class, new AABB(BlockPos.containing(trial.center)).inflate(ARENA_RADIUS + 8.0D),
            p -> p != player && p.isAlive() && !p.isSpectator()).size());
        List<EntityType<? extends Mob>> wave = new ArrayList<>();
        switch (trial.wave) {
            case 1 -> {
                for (int i = 0; i < 3; i++) {
                    wave.add(ModEntities.AURA_THRALL.get());
                }
            }
            case 2 -> {
                for (int i = 0; i < 3; i++) {
                    wave.add(ModEntities.AURA_THRALL.get());
                }
                wave.add(ModEntities.DEMON_SOLDIER.get());
            }
            default -> {
                wave.add(ModEntities.DEMON_SOLDIER.get());
                wave.add(ModEntities.DEMON_SOLDIER.get());
                wave.add(ModEntities.DEMON_MAGE.get());
            }
        }
        for (int i = 0; i < helpers; i++) {
            wave.add(trial.wave >= 3 ? ModEntities.DEMON_SOLDIER.get() : ModEntities.AURA_THRALL.get());
        }
        for (EntityType<? extends Mob> type : wave) {
            spawn(trial, level, player, type, true);
        }
    }

    /** Shows progress on the action bar when it changes. */
    private static void report(Trial trial, ServerPlayer player, int key, Component message) {
        if (trial.lastReported != key) {
            trial.lastReported = key;
            Messages.actionBar(player, message);
        }
    }

    /** Tracked entities that vanished without dying (unloaded, removed) are replaced after a few seconds. */
    private static void replaceMissing(Trial trial, ServerLevel level, ServerPlayer player) {
        List<UUID> lost = new ArrayList<>();
        for (Map.Entry<UUID, EntityType<?>> entry : trial.tracked.entrySet()) {
            UUID id = entry.getKey();
            if (trial.defeated.contains(id)) {
                continue;
            }
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                trial.missingTicks.remove(id);
                continue;
            }
            int missing = trial.missingTicks.merge(id, 1, Integer::sum);
            if (missing >= 6) {
                lost.add(id);
            }
        }
        for (UUID id : lost) {
            EntityType<?> type = trial.tracked.remove(id);
            trial.missingTicks.remove(id);
            if (type != null) {
                spawn(trial, level, player, type, true);
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Spawning
    // ---------------------------------------------------------------------------------------

    private static void spawn(Trial trial, ServerLevel level, ServerPlayer player, EntityType<?> type, boolean tracked) {
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (entity == null) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 forward = Vec3.directionFromRotation(0.0F, trial.facing);
        if (entity instanceof ExamConstructEntity construct) {
            Vec3 anchor = trial.center.add(0.0D, 2.0D + random.nextDouble() * 2.0D, 0.0D);
            double speed = (0.008D + random.nextDouble() * 0.012D) * (random.nextBoolean() ? 1.0D : -1.0D);
            construct.setOrbit(anchor, 4.0D + random.nextDouble() * 5.0D, random.nextDouble() * Math.PI * 2.0D, speed);
        } else if (entity instanceof MimicEntity mimic) {
            Vec3 pos = groundNear(level, trial.center, 4.0D, 8.0D, random);
            mimic.snapTo(pos.x, pos.y, pos.z, random.nextInt(4) * 90.0F, 0.0F);
            mimic.setYHeadRot(mimic.getYRot());
            mimic.setYBodyRot(mimic.getYRot());
            mimic.setTrueChest(tracked);
        } else if (entity instanceof StilleEntity stille) {
            stille.snapTo(trial.center.x + 4.0D, trial.center.y + 3.0D, trial.center.z + 4.0D, random.nextFloat() * 360.0F, 0.0F);
            stille.setExaminee(trial.examinee);
        } else if (entity instanceof SpiegelEntity || entity instanceof MirrorFrierenEntity || entity instanceof SerieEntity) {
            double ahead = entity instanceof SerieEntity ? 3.0D : 8.0D;
            Vec3 pos = groundAt(level, trial.center.add(forward.x * ahead, 0.0D, forward.z * ahead));
            entity.snapTo(pos.x, pos.y, pos.z, trial.facing + 180.0F, 0.0F);
            if (entity instanceof SerieEntity serie) {
                serie.setTemporary(true);
            }
        } else {
            Vec3 pos = groundNear(level, trial.center, ARENA_RADIUS - 3.0D, ARENA_RADIUS, random);
            entity.snapTo(pos.x, pos.y, pos.z, random.nextFloat() * 360.0F, 0.0F);
        }
        entity.addTag(TAG);
        entity.addTag(ownerTag(trial.examinee));
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            if (!(entity instanceof MimicEntity) && !(entity instanceof SerieEntity) && !(entity instanceof StilleEntity)
                    && !(entity instanceof ExamConstructEntity)) {
                mob.setTarget(player);
            }
        }
        if (!level.addFreshEntity(entity)) {
            FrierenMod.LOGGER.warn("[trial] could not add {} for {}", type, trial.examinee);
            return;
        }
        if (tracked) {
            trial.tracked.put(entity.getUUID(), type);
        } else {
            trial.props.add(entity.getUUID());
        }
        SpellFx.burst(level, entity.position().add(0.0D, entity.getBbHeight() / 2.0D, 0.0D), ARENA_COLOR, 0.6F);
    }

    /** A standable spot on a ring around {@code center} near its height; falls back to the centre. */
    private static Vec3 groundNear(ServerLevel level, Vec3 center, double minRadius, double maxRadius, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            Vec3 found = Ground.standable(level, center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius));
            if (found != null) {
                return found;
            }
        }
        return center;
    }

    private static Vec3 groundAt(ServerLevel level, Vec3 at) {
        Vec3 found = Ground.standable(level, at);
        return found != null ? found : at;
    }

    // ---------------------------------------------------------------------------------------
    // Items
    // ---------------------------------------------------------------------------------------

    private static boolean hasEmptyCage(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.STILLE_CAGE.get()) && !StilleCageItem.isFull(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** A line for the Exam Seal: what is left to do in the current trial. */
    public static @Nullable Component status(UUID examinee) {
        Trial trial = ACTIVE.get(examinee);
        if (trial == null) {
            return null;
        }
        return switch (trial.stage) {
            case PRACTICAL -> Component.translatable("exam.frieren.trial.constructs", CONSTRUCTS - trial.remaining(), CONSTRUCTS);
            case SURVIVAL -> Component.translatable("exam.frieren.trial.wave_progress", Math.max(1, trial.wave), SURVIVAL_WAVES, trial.remaining());
            default -> null;
        };
    }
}
