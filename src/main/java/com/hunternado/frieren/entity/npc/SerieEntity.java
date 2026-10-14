package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.exam.ExamType;
import com.hunternado.frieren.magic.MageRank;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.OpenSpellForgePacket;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.registry.ModTriggers;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serie, founder of the Continental Magic Association and a living grimoire from the Age of Myths.
 * <ul>
 *   <li>She conducts the First-Class interview: her mana is perfectly concealed except for a brief flicker every
 *   eight seconds, and noticing that flicker is the test.</li>
 *   <li>First-Class mages can call her with her letter. Talk to her and she appraises you: for 90 seconds every
 *   different spell you cast near her scores its rarity (common 1 ... mythic 10) times half-plus-mastery, half again
 *   if it hit something; releasing your limiter in front of her is worth 8. Reach {@link #APPRAISAL_GOAL} and she is
 *   impressed.</li>
 *   <li>Impressed, and with Übel beaten, she opens her spell forge and creates an original spell for you.</li>
 * </ul>
 */
public class SerieEntity extends FrierenNpcBase {
    private static final int FLICKER_PERIOD = 160;
    private static final int FLICKER_LENGTH = 30;
    public static final float APPRAISAL_GOAL = 30.0F;
    private static final int APPRAISAL_TICKS = 1800;
    private static final double APPRAISAL_RANGE = 24.0D;
    /** Players who bored her recently cannot ask again for a minute (by player id, game time). */
    private static final Map<UUID, Long> RETRY_AT = new ConcurrentHashMap<>();

    private final ServerBossEvent interest = new ServerBossEvent(UUID.randomUUID(), Component.translatable("boss.frieren.serie.interest"),
        BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable UUID appraising;
    private int appraisalTicks;
    private float score;
    private int remarks;
    private final Set<Identifier> seen = new HashSet<>();

    public SerieEntity(EntityType<? extends SerieEntity> type, Level level) {
        super(type, level);
    }

    /** Spawned for an interview: leaves again after five minutes. */
    public void setTemporary(boolean temporary) {
        if (temporary) {
            setDepartAt(this.level().getGameTime() + 6000L);
        }
    }

    /** Called by her letter: she stays {@code ticks} and then goes home. */
    public void setVisit(long ticks) {
        setDepartAt(this.level().getGameTime() + ticks);
    }

    public boolean isFlickering() {
        return this.tickCount % FLICKER_PERIOD < FLICKER_LENGTH;
    }

    public int flickerConcealLevel() {
        return 4;
    }

    @Override
    protected boolean wanders() {
        return false;
    }

    @Override
    protected String npcId() {
        return "serie";
    }

    @Override
    public float trueMana() {
        return 5000.0F;
    }

    @Override
    public int concealLevel() {
        return isFlickering() ? flickerConcealLevel() : 12;
    }

    // ---------------------------------------------------------------------------------------
    // Talking
    // ---------------------------------------------------------------------------------------

    @Override
    public void onInteract(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        ExamType type = data != null && data.exam() != null ? ExamType.forRank(data.exam().target()) : null;
        if (type != null && type.stage(data.exam().stage()) == ExamType.Stage.INTERVIEW) {
            Messages.chat(player, Component.translatable("npc.frieren.serie.interview").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (data == null || data.rank() != MageRank.FIRST_CLASS) {
            sayRandomLine(player);
            return;
        }
        if (!data.serieImpressed()) {
            if (appraising != null && appraising.equals(player.getUUID())) {
                say(player, "appraisal_running");
            } else if (appraising != null) {
                say(player, "appraisal_busy");
            } else {
                Long retry = RETRY_AT.get(player.getUUID());
                if (retry != null && this.level().getGameTime() < retry) {
                    say(player, "appraisal_wait");
                } else {
                    beginAppraisal(player);
                }
            }
            return;
        }
        if (!data.ubelDefeated()) {
            say(player, "needs_ubel");
            return;
        }
        say(player, "forge_open");
        FrierenNetwork.sendToPlayer(player, new OpenSpellForgePacket(this.getId()));
    }

    private void say(ServerPlayer player, String key, Object... args) {
        Messages.chat(player, Component.literal("<").append(this.getDisplayName()).append("> ")
            .append(Component.translatable("npc.frieren.serie." + key, args).withStyle(ChatFormatting.WHITE)));
    }

    // ---------------------------------------------------------------------------------------
    // The appraisal
    // ---------------------------------------------------------------------------------------

    private void beginAppraisal(ServerPlayer player) {
        appraising = player.getUUID();
        appraisalTicks = APPRAISAL_TICKS;
        score = 0.0F;
        remarks = 0;
        seen.clear();
        interest.setProgress(0.0F);
        interest.addPlayer(player);
        say(player, "appraisal_start", APPRAISAL_TICKS / 20);
    }

    private void endAppraisal() {
        appraising = null;
        appraisalTicks = 0;
        interest.removeAllPlayers();
    }

    /** SpellCaster calls this after every successful cast: Serie scores each different spell once. */
    public static void onSpellCast(ServerPlayer player, SpellDefinition spell, int hits, float mastery) {
        for (SerieEntity serie : player.level().getEntitiesOfClass(SerieEntity.class,
                new AABB(player.blockPosition()).inflate(APPRAISAL_RANGE), serie -> player.getUUID().equals(serie.appraising))) {
            if (serie.seen.add(spell.id())) {
                float points = rarityValue(spell) * (0.5F + mastery) * (hits > 0 ? 1.5F : 1.0F);
                serie.addScore(player, points);
            }
        }
    }

    /** A limiter released in front of her: she notices (8 points). */
    public static void onLimiterRelease(ServerPlayer player) {
        for (SerieEntity serie : player.level().getEntitiesOfClass(SerieEntity.class,
                new AABB(player.blockPosition()).inflate(APPRAISAL_RANGE), serie -> player.getUUID().equals(serie.appraising))) {
            if (serie.seen.add(Identifier.withDefaultNamespace("limiter_release"))) {
                serie.say(player, "noticed_limiter");
                serie.addScore(player, 8.0F);
            }
        }
    }

    private static float rarityValue(SpellDefinition spell) {
        return switch (spell.rarity()) {
            case COMMON -> 1.0F;
            case UNCOMMON -> 2.0F;
            case RARE -> 3.0F;
            case ANCIENT -> 5.0F;
            case LEGENDARY -> 8.0F;
            case DEMON -> 4.0F;
            case FORBIDDEN -> 7.0F;
            case MYTHIC -> 10.0F;
        };
    }

    private void addScore(ServerPlayer player, float points) {
        score += points;
        float progress = Math.min(1.0F, score / APPRAISAL_GOAL);
        interest.setProgress(progress);
        int stage = (int) (progress * 4.0F);
        if (stage > remarks && stage < 4) {
            remarks = stage;
            say(player, "appraisal_" + stage);
        }
        if (score >= APPRAISAL_GOAL && this.level() instanceof ServerLevel level) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.setSerieImpressed(true);
            }
            endAppraisal();
            say(player, data != null && data.ubelDefeated() ? "impressed_ready" : "impressed");
            SpellFx.petals(level, this.position(), 0xF6CB58, 2.5F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.RANK_UP.get(), SoundSource.NEUTRAL, 0.8F, 1.3F);
            ModTriggers.milestone(player, "serie_impressed");
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level) || appraising == null) {
            return;
        }
        Entity entity = level.getEntity(appraising);
        if (!(entity instanceof ServerPlayer player) || player.distanceToSqr(this) > APPRAISAL_RANGE * APPRAISAL_RANGE * 4.0D) {
            endAppraisal();
            return;
        }
        this.getLookControl().setLookAt(player, 10.0F, 10.0F);
        interest.setProgress(Math.min(1.0F, score / APPRAISAL_GOAL));
        if (--appraisalTicks <= 0) {
            say(player, "appraisal_bored");
            RETRY_AT.put(player.getUUID(), level.getGameTime() + 1200L);
            endAppraisal();
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        interest.removeAllPlayers();
    }
}
