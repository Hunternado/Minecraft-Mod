package com.hunternado.frieren.data;

import com.hunternado.frieren.magic.MageRank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * All magical progression of a single player.
 * <p>
 * Server instance: authoritative, persisted through the Forge capability system.
 * Client instance: a mirror inside {@code ClientMagicCache}, overwritten by sync packets.
 * This class has no side-specific code.
 */
@AutoRegisterCapability
public final class MagicData {
    public static final int QUICK_SLOTS = 5;

    // ---- Mana -------------------------------------------------------------------------------
    private float mana = 50.0F;
    private int cachedMaxMana = 50;
    private int manaGrowth;
    private int bonusGrowth;
    private float trainingXp;

    // ---- Rank / spells ----------------------------------------------------------------------
    private MageRank rank = MageRank.UNRANKED;
    private final Map<Identifier, SpellProgress> knownSpells = new LinkedHashMap<>();
    private final Identifier[] slots = new Identifier[QUICK_SLOTS];
    private int selectedSlot;
    /** Spell -> game time at which it is ready again. */
    private final Map<Identifier, Long> cooldownEnds = new HashMap<>();

    // ---- Concealment & detection ------------------------------------------------------------
    private boolean concealing;
    private float concealMastery;
    private float detectionMastery;

    // ---- Active magic (transient, never persisted) -------------------------------------------
    private @Nullable Identifier flightSpell;
    private @Nullable Identifier barrierSpell;
    private @Nullable Identifier channelingSpell;
    private int channelTicks;
    private @Nullable Identifier pendingCast;
    private int castTicksRemaining;
    private long regenPausedUntil;
    private long lastDetectTime = -1000L;
    private long enthralledUntil;

    // ---- Equipment cache (transient, refreshed once per second by ManaHelper) ---------------
    private int attirePieces;
    private boolean mirrorLotusRing;
    private boolean concealmentCharm;
    private boolean demonHunterInsignia;

    // ---- Exams, quests, stats ---------------------------------------------------------------
    private @Nullable ExamProgress exam;
    private final Set<MageRank> passedExams = EnumSet.noneOf(MageRank.class);
    private final Map<String, Integer> questProgress = new HashMap<>();
    private final Set<String> completedQuests = new HashSet<>();
    private final Map<String, Integer> stats = new HashMap<>();
    private boolean starterGiven;
    private boolean seriePrivilegeAvailable;
    private @Nullable String lastDeathDimension;
    private @Nullable BlockPos lastDeathPos;

    // ---- Sync bookkeeping (transient) -------------------------------------------------------
    private boolean fullSyncNeeded = true;
    private float lastSyncedMana = -1.0F;
    private int lastSyncedMaxMana = -1;

    public static @Nullable MagicData get(Player player) {
        return player.getCapability(MagicDataProvider.MAGIC_DATA).orElse(null);
    }

    // =========================================================================================
    // Mana
    // =========================================================================================

    public float mana() { return mana; }
    public int maxMana() { return cachedMaxMana; }
    public int manaGrowth() { return manaGrowth; }
    public int bonusGrowth() { return bonusGrowth; }
    public float trainingXp() { return trainingXp; }

    public void setMana(float mana) {
        this.mana = Math.max(0.0F, Math.min(cachedMaxMana, mana));
    }

    public void setCachedMaxMana(int maxMana) {
        this.cachedMaxMana = Math.max(1, maxMana);
        if (this.mana > this.cachedMaxMana) {
            this.mana = this.cachedMaxMana;
        }
    }

    public boolean trySpendMana(float amount) {
        if (amount <= 0.0F) {
            return true;
        }
        if (mana + 1.0E-4F < amount) {
            return false;
        }
        mana = Math.max(0.0F, mana - amount);
        return true;
    }

    public void setManaGrowth(int growth) { this.manaGrowth = Math.max(0, growth); markDirty(); }
    public void setBonusGrowth(int growth) { this.bonusGrowth = Math.max(0, growth); markDirty(); }
    public void setTrainingXp(float xp) { this.trainingXp = Math.max(0.0F, xp); }

    public long regenPausedUntil() { return regenPausedUntil; }
    public void pauseRegenUntil(long gameTime) { this.regenPausedUntil = Math.max(regenPausedUntil, gameTime); }

    // =========================================================================================
    // Rank & spells
    // =========================================================================================

    public MageRank rank() { return rank; }

    public void setRank(MageRank rank) {
        this.rank = rank;
        markDirty();
    }

    public boolean knows(Identifier spell) {
        return knownSpells.containsKey(spell);
    }

    public Map<Identifier, SpellProgress> knownSpells() {
        return Collections.unmodifiableMap(knownSpells);
    }

    public int knownCount() {
        return knownSpells.size();
    }

    /** @return true if the spell was newly learned. */
    public boolean learn(Identifier spell) {
        if (knownSpells.containsKey(spell)) {
            return false;
        }
        knownSpells.put(spell, new SpellProgress());
        // Auto-bind into the first free slot so a new mage can cast right away.
        for (int i = 0; i < QUICK_SLOTS; i++) {
            if (slots[i] == null) {
                slots[i] = spell;
                break;
            }
        }
        markDirty();
        return true;
    }

    public boolean forget(Identifier spell) {
        if (knownSpells.remove(spell) == null) {
            return false;
        }
        for (int i = 0; i < QUICK_SLOTS; i++) {
            if (spell.equals(slots[i])) {
                slots[i] = null;
            }
        }
        cooldownEnds.remove(spell);
        markDirty();
        return true;
    }

    public void forgetAll() {
        knownSpells.clear();
        Arrays.fill(slots, null);
        cooldownEnds.clear();
        markDirty();
    }

    public @Nullable SpellProgress progress(Identifier spell) {
        return knownSpells.get(spell);
    }

    public int masteryCount(int atLeast) {
        int count = 0;
        for (SpellProgress progress : knownSpells.values()) {
            if (progress.mastery() >= atLeast) {
                count++;
            }
        }
        return count;
    }

    public @Nullable Identifier slot(int index) {
        return index >= 0 && index < QUICK_SLOTS ? slots[index] : null;
    }

    public void setSlot(int index, @Nullable Identifier spell) {
        if (index < 0 || index >= QUICK_SLOTS) {
            return;
        }
        if (spell != null) {
            // A spell occupies at most one slot.
            for (int i = 0; i < QUICK_SLOTS; i++) {
                if (spell.equals(slots[i])) {
                    slots[i] = null;
                }
            }
        }
        slots[index] = spell;
        markDirty();
    }

    public int selectedSlot() { return selectedSlot; }

    public void setSelectedSlot(int slot) {
        this.selectedSlot = Math.floorMod(slot, QUICK_SLOTS);
        markDirty();
    }

    public @Nullable Identifier selectedSpell() {
        return slots[selectedSlot];
    }

    public long cooldownEnd(Identifier spell) {
        return cooldownEnds.getOrDefault(spell, 0L);
    }

    public int cooldownRemaining(Identifier spell, long gameTime) {
        return (int) Math.max(0L, cooldownEnd(spell) - gameTime);
    }

    public void setCooldown(Identifier spell, long readyAt) {
        cooldownEnds.put(spell, readyAt);
    }

    public Map<Identifier, Long> cooldowns() {
        return Collections.unmodifiableMap(cooldownEnds);
    }

    public void clearCooldowns() {
        cooldownEnds.clear();
        markDirty();
    }

    // =========================================================================================
    // Concealment / detection
    // =========================================================================================

    public boolean concealing() { return concealing; }
    public void setConcealing(boolean concealing) { this.concealing = concealing; markDirty(); }
    public float concealMastery() { return concealMastery; }
    public void setConcealMastery(float value) { this.concealMastery = Math.max(0.0F, Math.min(100.0F, value)); }
    public float detectionMastery() { return detectionMastery; }
    public void setDetectionMastery(float value) { this.detectionMastery = Math.max(0.0F, Math.min(100.0F, value)); }
    public long lastDetectTime() { return lastDetectTime; }
    public void setLastDetectTime(long time) { this.lastDetectTime = time; }

    // =========================================================================================
    // Active magic state
    // =========================================================================================

    public boolean flightActive() { return flightSpell != null; }
    public @Nullable Identifier flightSpell() { return flightSpell; }
    public void setFlightSpell(@Nullable Identifier spell) { this.flightSpell = spell; markDirty(); }
    public boolean barrierActive() { return barrierSpell != null; }
    public @Nullable Identifier barrierSpell() { return barrierSpell; }
    public void setBarrierSpell(@Nullable Identifier spell) { this.barrierSpell = spell; markDirty(); }

    public @Nullable Identifier channelingSpell() { return channelingSpell; }
    public int channelTicks() { return channelTicks; }

    public void startChannel(Identifier spell) {
        this.channelingSpell = spell;
        this.channelTicks = 0;
    }

    public void tickChannel() { this.channelTicks++; }

    public void stopChannel() {
        this.channelingSpell = null;
        this.channelTicks = 0;
    }

    public @Nullable Identifier pendingCast() { return pendingCast; }
    public int castTicksRemaining() { return castTicksRemaining; }

    public void beginCast(Identifier spell, int ticks) {
        this.pendingCast = spell;
        this.castTicksRemaining = ticks;
    }

    public void tickCast() { this.castTicksRemaining--; }

    public void clearPendingCast() {
        this.pendingCast = null;
        this.castTicksRemaining = 0;
    }

    public long enthralledUntil() { return enthralledUntil; }
    public void setEnthralledUntil(long time) { this.enthralledUntil = time; }

    public int attirePieces() { return attirePieces; }
    public boolean hasMirrorLotusRing() { return mirrorLotusRing; }
    public boolean hasConcealmentCharm() { return concealmentCharm; }
    public boolean hasDemonHunterInsignia() { return demonHunterInsignia; }

    public void setEquipment(int attirePieces, boolean ring, boolean charm, boolean insignia) {
        this.attirePieces = attirePieces;
        this.mirrorLotusRing = ring;
        this.concealmentCharm = charm;
        this.demonHunterInsignia = insignia;
    }

    /** Clears everything that must not survive death, dimension change or relog. */
    public void resetTransient() {
        flightSpell = null;
        barrierSpell = null;
        channelingSpell = null;
        channelTicks = 0;
        pendingCast = null;
        castTicksRemaining = 0;
        enthralledUntil = 0L;
        markDirty();
    }

    // =========================================================================================
    // Exams / quests / stats
    // =========================================================================================

    public @Nullable ExamProgress exam() { return exam; }

    public void setExam(@Nullable ExamProgress exam) {
        this.exam = exam;
        markDirty();
    }

    public boolean hasPassed(MageRank rank) { return passedExams.contains(rank); }
    public void markPassed(MageRank rank) { passedExams.add(rank); markDirty(); }

    public int questProgress(String quest) { return questProgress.getOrDefault(quest, 0); }

    public void setQuestProgress(String quest, int value) {
        questProgress.put(quest, value);
        markDirty();
    }

    public boolean questCompleted(String quest) { return completedQuests.contains(quest); }

    public void completeQuest(String quest) {
        completedQuests.add(quest);
        markDirty();
    }

    public void resetQuest(String quest) {
        completedQuests.remove(quest);
        questProgress.remove(quest);
        markDirty();
    }

    public Set<String> completedQuests() { return Collections.unmodifiableSet(completedQuests); }

    public int stat(String stat) { return stats.getOrDefault(stat, 0); }

    public void setStat(String stat, int value) {
        stats.put(stat, value);
        markDirty();
    }

    public int incrementStat(String stat) {
        int value = stats.getOrDefault(stat, 0) + 1;
        stats.put(stat, value);
        markDirty();
        return value;
    }

    public boolean starterGiven() { return starterGiven; }
    public void setStarterGiven(boolean given) { this.starterGiven = given; }
    public boolean seriePrivilegeAvailable() { return seriePrivilegeAvailable; }
    public void setSeriePrivilegeAvailable(boolean available) { this.seriePrivilegeAvailable = available; markDirty(); }

    public @Nullable String lastDeathDimension() { return lastDeathDimension; }
    public @Nullable BlockPos lastDeathPos() { return lastDeathPos; }

    public void setLastDeath(String dimension, BlockPos pos) {
        this.lastDeathDimension = dimension;
        this.lastDeathPos = pos.immutable();
    }

    // =========================================================================================
    // Sync bookkeeping
    // =========================================================================================

    public void markDirty() { this.fullSyncNeeded = true; }
    public boolean fullSyncNeeded() { return fullSyncNeeded; }
    public void clearFullSync() { this.fullSyncNeeded = false; }
    public float lastSyncedMana() { return lastSyncedMana; }
    public int lastSyncedMaxMana() { return lastSyncedMaxMana; }

    public void markManaSynced() {
        this.lastSyncedMana = mana;
        this.lastSyncedMaxMana = cachedMaxMana;
    }

    // =========================================================================================
    // Persistence
    // =========================================================================================

    /** Full persistent state. Also used as the network snapshot (it is small). */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mana", mana);
        tag.putInt("maxMana", cachedMaxMana);
        tag.putInt("growth", manaGrowth);
        tag.putInt("bonusGrowth", bonusGrowth);
        tag.putFloat("trainingXp", trainingXp);
        tag.putString("rank", rank.serializedName());

        CompoundTag spells = new CompoundTag();
        knownSpells.forEach((id, progress) -> spells.put(id.toString(), progress.save()));
        tag.put("spells", spells);

        CompoundTag slotTag = new CompoundTag();
        for (int i = 0; i < QUICK_SLOTS; i++) {
            if (slots[i] != null) {
                slotTag.putString(Integer.toString(i), slots[i].toString());
            }
        }
        tag.put("slots", slotTag);
        tag.putInt("selectedSlot", selectedSlot);

        CompoundTag cooldownTag = new CompoundTag();
        cooldownEnds.forEach((id, end) -> cooldownTag.putLong(id.toString(), end));
        tag.put("cooldowns", cooldownTag);

        tag.putBoolean("concealing", concealing);
        tag.putFloat("concealMastery", concealMastery);
        tag.putFloat("detectionMastery", detectionMastery);
        if (flightSpell != null) {
            tag.putString("flight", flightSpell.toString());
        }
        if (barrierSpell != null) {
            tag.putString("barrier", barrierSpell.toString());
        }

        if (exam != null) {
            tag.put("exam", exam.save());
        }
        CompoundTag passed = new CompoundTag();
        for (MageRank passedRank : passedExams) {
            passed.putBoolean(passedRank.serializedName(), true);
        }
        tag.put("passedExams", passed);

        CompoundTag questTag = new CompoundTag();
        questProgress.forEach(questTag::putInt);
        tag.put("questProgress", questTag);
        CompoundTag completedTag = new CompoundTag();
        for (String quest : completedQuests) {
            completedTag.putBoolean(quest, true);
        }
        tag.put("completedQuests", completedTag);
        CompoundTag statTag = new CompoundTag();
        stats.forEach(statTag::putInt);
        tag.put("stats", statTag);

        tag.putBoolean("starterGiven", starterGiven);
        tag.putBoolean("seriePrivilege", seriePrivilegeAvailable);
        if (lastDeathDimension != null && lastDeathPos != null) {
            tag.putString("deathDim", lastDeathDimension);
            tag.putLong("deathPos", lastDeathPos.asLong());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        cachedMaxMana = Math.max(1, tag.getIntOr("maxMana", 50));
        mana = tag.getFloatOr("mana", cachedMaxMana);
        manaGrowth = tag.getIntOr("growth", 0);
        bonusGrowth = tag.getIntOr("bonusGrowth", 0);
        trainingXp = tag.getFloatOr("trainingXp", 0.0F);
        rank = MageRank.byName(tag.getStringOr("rank", MageRank.UNRANKED.serializedName()));

        knownSpells.clear();
        CompoundTag spells = tag.getCompoundOrEmpty("spells");
        for (String key : spells.keySet()) {
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                knownSpells.put(id, SpellProgress.load(spells.getCompoundOrEmpty(key)));
            }
        }

        Arrays.fill(slots, null);
        CompoundTag slotTag = tag.getCompoundOrEmpty("slots");
        for (int i = 0; i < QUICK_SLOTS; i++) {
            String value = slotTag.getStringOr(Integer.toString(i), "");
            if (!value.isEmpty()) {
                slots[i] = Identifier.tryParse(value);
            }
        }
        selectedSlot = Math.floorMod(tag.getIntOr("selectedSlot", 0), QUICK_SLOTS);

        cooldownEnds.clear();
        CompoundTag cooldownTag = tag.getCompoundOrEmpty("cooldowns");
        for (String key : cooldownTag.keySet()) {
            Identifier id = Identifier.tryParse(key);
            if (id != null) {
                cooldownEnds.put(id, cooldownTag.getLongOr(key, 0L));
            }
        }

        concealing = tag.getBooleanOr("concealing", false);
        concealMastery = tag.getFloatOr("concealMastery", 0.0F);
        detectionMastery = tag.getFloatOr("detectionMastery", 0.0F);
        String flight = tag.getStringOr("flight", "");
        flightSpell = flight.isEmpty() ? null : Identifier.tryParse(flight);
        String barrier = tag.getStringOr("barrier", "");
        barrierSpell = barrier.isEmpty() ? null : Identifier.tryParse(barrier);

        exam = tag.contains("exam") ? ExamProgress.load(tag.getCompoundOrEmpty("exam")) : null;
        passedExams.clear();
        CompoundTag passed = tag.getCompoundOrEmpty("passedExams");
        for (String key : passed.keySet()) {
            MageRank passedRank = MageRank.byName(key);
            if (passedRank != MageRank.UNRANKED) {
                passedExams.add(passedRank);
            }
        }

        questProgress.clear();
        CompoundTag questTag = tag.getCompoundOrEmpty("questProgress");
        for (String key : questTag.keySet()) {
            questProgress.put(key, questTag.getIntOr(key, 0));
        }
        completedQuests.clear();
        completedQuests.addAll(tag.getCompoundOrEmpty("completedQuests").keySet());
        stats.clear();
        CompoundTag statTag = tag.getCompoundOrEmpty("stats");
        for (String key : statTag.keySet()) {
            stats.put(key, statTag.getIntOr(key, 0));
        }

        starterGiven = tag.getBooleanOr("starterGiven", false);
        seriePrivilegeAvailable = tag.getBooleanOr("seriePrivilege", false);
        if (tag.contains("deathDim")) {
            lastDeathDimension = tag.getStringOr("deathDim", "");
            lastDeathPos = BlockPos.of(tag.getLongOr("deathPos", 0L));
        } else {
            lastDeathDimension = null;
            lastDeathPos = null;
        }
        markDirty();
    }

    /** Respawn / end-portal copy. Progression always survives; mana is refilled to half on death. */
    public void copyFrom(MagicData other, boolean wasDeath) {
        load(other.save());
        resetTransient();
        if (wasDeath) {
            mana = cachedMaxMana * 0.5F;
            concealing = false;
        }
    }
}
