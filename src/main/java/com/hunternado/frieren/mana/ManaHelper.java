package com.hunternado.frieren.mana;

import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.item.ArtifactItem;
import com.hunternado.frieren.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * All mana formulas live here so balance can be read (and tuned) in one place.
 */
public final class ManaHelper {
    /** Mana attire bonus per worn piece, plus a set bonus for all four. */
    public static final int ATTIRE_MAX_MANA_PER_PIECE = 12;
    public static final int ATTIRE_SET_BONUS = 25;
    public static final int ABSOLUTE_BONUS_GROWTH_CAP = 400;

    private ManaHelper() {}

    // ---------------------------------------------------------------------------------------
    // Capacity
    // ---------------------------------------------------------------------------------------

    public static int computeMaxMana(MagicData data) {
        int growth = Math.min(data.manaGrowth(), data.rank().growthCap()) + data.bonusGrowth();
        int attire = data.attirePieces() * ATTIRE_MAX_MANA_PER_PIECE + (data.attirePieces() >= 4 ? ATTIRE_SET_BONUS : 0);
        return FrierenConfig.baseMaxMana() + growth + data.rank().maxManaBonus() + attire;
    }

    /** Training XP needed for the next point of growth. */
    public static float xpForNextGrowth(int growth) {
        return 8.0F + growth * 0.4F;
    }

    // ---------------------------------------------------------------------------------------
    // Regeneration
    // ---------------------------------------------------------------------------------------

    public static boolean isMeditating(Player player) {
        return player.isShiftKeyDown() && player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
    }

    public static float regenPerTick(Player player, MagicData data) {
        float regen = 0.03F + data.maxMana() * 0.00035F;
        regen *= (float) FrierenConfig.manaRegenMultiplier();
        regen *= data.rank().regenMultiplier();
        regen *= 1.0F + 0.05F * data.attirePieces();
        if (data.hasMirrorLotusRing()) {
            regen *= 1.25F;
        }
        if (isMeditating(player)) {
            regen *= 2.0F;
        }
        if (data.concealing()) {
            regen *= 0.5F;
        }
        return regen;
    }

    // ---------------------------------------------------------------------------------------
    // Concealment & detection
    // ---------------------------------------------------------------------------------------

    /** Fraction (0..0.97) of the true signature hidden while concealing. */
    public static float concealFraction(MagicData data) {
        if (!data.concealing()) {
            return 0.0F;
        }
        float fraction = 0.3F + 0.65F * (data.concealMastery() / 100.0F);
        if (data.hasConcealmentCharm()) {
            fraction += 0.15F;
        }
        return Math.min(0.97F, fraction);
    }

    /** What other mages and demons perceive. */
    public static float apparentMana(MagicData data) {
        return data.maxMana() * (1.0F - concealFraction(data));
    }

    /** Abstract detection strength (1..~11) compared against concealment levels. */
    public static int detectionPower(MagicData data) {
        return data.rank().detectionPower() + (int) (data.detectionMastery() / 25.0F);
    }

    public static double detectionRadius(MagicData data) {
        return 24.0D + data.rank().ordinal() * 4.0D;
    }

    // ---------------------------------------------------------------------------------------
    // Server tick & training
    // ---------------------------------------------------------------------------------------

    /** Runs every server tick for each player. Cheap: heavy work happens once per second. */
    public static void serverTick(ServerPlayer player, MagicData data) {
        long time = player.level().getGameTime();

        if (time % 20L == 0L) {
            refreshEquipment(player, data);
            data.setCachedMaxMana(computeMaxMana(data));
            if (data.concealing()) {
                float gain = data.hasConcealmentCharm() ? 0.04F : 0.02F;
                if (isMeditating(player)) {
                    gain += 0.01F;
                }
                data.setConcealMastery(data.concealMastery() + gain);
            }
        }

        if (time >= data.regenPausedUntil() && data.channelingSpell() == null && data.mana() < data.maxMana()) {
            data.setMana(data.mana() + regenPerTick(player, data));
        }
    }

    /** Spending mana is the training that raises capacity (bounded by rank). */
    public static void onManaSpent(ServerPlayer player, MagicData data, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        data.setTrainingXp(data.trainingXp() + amount * (float) FrierenConfig.manaGrowthMultiplier());
        int cap = data.rank().growthCap();
        boolean grew = false;
        while (data.manaGrowth() < cap && data.trainingXp() >= xpForNextGrowth(data.manaGrowth())) {
            data.setTrainingXp(data.trainingXp() - xpForNextGrowth(data.manaGrowth()));
            data.setManaGrowth(data.manaGrowth() + 1);
            grew = true;
        }
        if (data.manaGrowth() >= cap) {
            // Training beyond the cap is wasted; keep the counter from growing without bound.
            data.setTrainingXp(Math.min(data.trainingXp(), xpForNextGrowth(cap)));
        }
        if (grew) {
            data.setCachedMaxMana(computeMaxMana(data));
            if (data.manaGrowth() % 10 == 0) {
                player.sendSystemMessage(Component.translatable("message.frieren.mana_grew", data.maxMana()));
            } else if (data.manaGrowth() == cap) {
                player.sendSystemMessage(Component.translatable("message.frieren.mana_capped"));
            }
        }
    }

    /** Permanent growth from bosses and quests; ignores the rank cap up to an absolute ceiling. */
    public static void grantBonusGrowth(ServerPlayer player, MagicData data, int amount) {
        data.setBonusGrowth(Math.min(ABSOLUTE_BONUS_GROWTH_CAP, data.bonusGrowth() + amount));
        data.setCachedMaxMana(computeMaxMana(data));
        player.sendSystemMessage(Component.translatable("message.frieren.mana_bonus", amount, data.maxMana()));
    }

    public static void refreshEquipment(Player player, MagicData data) {
        int attire = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (ModItems.isMageAttire(player.getItemBySlot(slot))) {
                attire++;
            }
        }
        boolean ring = false;
        boolean charm = false;
        boolean insignia = false;
        for (int i = 0; i < 10; i++) {
            ItemStack stack = i < 9 ? player.getInventory().getItem(i) : player.getOffhandItem();
            if (stack.getItem() instanceof ArtifactItem artifact) {
                switch (artifact.kind()) {
                    case MIRROR_LOTUS_RING -> ring = true;
                    case CONCEALMENT_CHARM -> charm = true;
                    case DEMON_HUNTER_INSIGNIA -> insignia = true;
                }
            }
        }
        data.setEquipment(attire, ring, charm, insignia);
    }
}
