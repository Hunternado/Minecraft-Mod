package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.exam.ExamManager;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.network.FrierenNetwork;
import com.hunternado.frieren.network.packet.ExaminerScreenPacket;
import com.hunternado.frieren.network.packet.NpcActionPacket;
import com.hunternado.frieren.network.packet.TradeScreenPacket;
import com.hunternado.frieren.spell.SpellDefinition;
import com.hunternado.frieren.spell.SpellManager;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Server-side routing for right-clicking friendly NPCs and mimics, and for the buttons of NPC screens.
 * Every action re-validates entity type and distance; the client never decides an outcome.
 */
public final class NpcInteractions {
    private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;

    private NpcInteractions() {}

    public static void register() {
        PlayerInteractEvent.EntityInteractSpecific.BUS.addListener((Predicate<PlayerInteractEvent.EntityInteractSpecific>) NpcInteractions::onInteract);
    }

    /** @return true to cancel vanilla handling. Runs on both sides; only the server acts. */
    private static boolean onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Entity target = event.getTarget();
        boolean ours = target instanceof FrierenNpcBase || (target instanceof MimicEntity mimic && !mimic.isRevealed());
        if (!ours || event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (target instanceof MimicEntity mimic) {
            mimic.chomp(player);
        } else if (target instanceof FrierenNpcBase npc) {
            npc.onInteract(player);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    // ---------------------------------------------------------------------------------------
    // Opening screens
    // ---------------------------------------------------------------------------------------

    public static void openTrades(ServerPlayer player, FrierenNpcBase npc) {
        List<TradeScreenPacket.Offer> offers = new ArrayList<>();
        for (NpcTrades.Offer offer : npc.offers()) {
            offers.add(new TradeScreenPacket.Offer(offer.costA(), offer.costB(), offer.result(), -1));
        }
        if (!offers.isEmpty()) {
            FrierenNetwork.sendToPlayer(player, new TradeScreenPacket(npc.getId(), npc.tradeTitleKey(), List.copyOf(offers)));
        }
    }

    public static void openExaminer(ServerPlayer player, ExaminerEntity examiner) {
        CompoundTag info = new CompoundTag();
        info.putLong("time", player.level().getGameTime());
        FrierenNetwork.sendToPlayer(player, new ExaminerScreenPacket(examiner.getId(), info));
    }

    // ---------------------------------------------------------------------------------------
    // Screen actions
    // ---------------------------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, int entityId, int action, int argument) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof FrierenNpcBase npc) || !npc.isAlive() || npc.distanceToSqr(player) > MAX_DISTANCE_SQR) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        switch (action) {
            case NpcActionPacket.EXAM_START -> {
                if (npc instanceof ExaminerEntity) {
                    ExamManager.start(player, data, npc);
                }
            }
            case NpcActionPacket.EXAM_ABANDON -> {
                if (npc instanceof ExaminerEntity) {
                    ExamManager.abandon(player, data);
                }
            }
            case NpcActionPacket.SUBMIT_EXAM_ITEM -> {
                if (npc instanceof ExaminerEntity) {
                    ExamManager.submitItem(player, data);
                }
            }
            case NpcActionPacket.CLAIM_PRIVILEGE -> {
                if (npc instanceof ExaminerEntity) {
                    claimPrivilege(player, data, argument);
                }
            }
            case NpcActionPacket.BUY_OFFER -> buy(player, npc, argument);
            default -> {
            }
        }
    }

    /** Legendary spells eligible for Serie's privilege, in a stable order shared with the client. */
    public static List<SpellDefinition> privilegeChoices(Iterable<SpellDefinition> definitions) {
        List<SpellDefinition> choices = new ArrayList<>();
        for (SpellDefinition definition : definitions) {
            if (definition.rarity() == SpellRarity.LEGENDARY) {
                choices.add(definition);
            }
        }
        return choices;
    }

    private static void claimPrivilege(ServerPlayer player, MagicData data, int index) {
        if (!data.seriePrivilegeAvailable()) {
            Messages.chat(player, Component.translatable("exam.frieren.privilege_none"));
            return;
        }
        List<SpellDefinition> choices = privilegeChoices(SpellManager.serverDefinitions());
        if (index < 0 || index >= choices.size()) {
            return;
        }
        data.setSeriePrivilegeAvailable(false);
        ItemStack grimoire = GrimoireItem.forSpell(choices.get(index));
        if (!player.getInventory().add(grimoire)) {
            player.drop(grimoire, false);
        }
        Messages.chat(player, Component.translatable("exam.frieren.privilege_claimed", choices.get(index).displayName())
            .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void buy(ServerPlayer player, FrierenNpcBase npc, int index) {
        List<NpcTrades.Offer> offers = npc.offers();
        if (index < 0 || index >= offers.size()) {
            return;
        }
        NpcTrades.Offer offer = offers.get(index);
        if (!hasItems(player, offer.costA()) || !hasItems(player, offer.costB())) {
            Messages.actionBar(player, Component.translatable("message.frieren.trade_cannot_afford"));
            return;
        }
        takeItems(player, offer.costA());
        takeItems(player, offer.costB());
        ItemStack result = offer.result().copy();
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        Messages.actionBar(player, Component.translatable("message.frieren.trade_done", offer.result().getHoverName()));
    }

    private static boolean hasItems(ServerPlayer player, ItemStack cost) {
        if (cost.isEmpty()) {
            return true;
        }
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack, cost)) {
                count += stack.getCount();
            }
        }
        return count >= cost.getCount();
    }

    private static void takeItems(ServerPlayer player, ItemStack cost) {
        if (cost.isEmpty()) {
            return;
        }
        int remaining = cost.getCount();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack, cost)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    /** Same item; for grimoires the taught spell must match too (or the cost must be unidentified). */
    private static boolean matches(ItemStack stack, ItemStack cost) {
        if (!stack.is(cost.getItem())) {
            return false;
        }
        if (cost.getItem() instanceof GrimoireItem) {
            var wanted = GrimoireItem.spellOf(cost);
            return wanted == null || wanted.equals(GrimoireItem.spellOf(stack));
        }
        return true;
    }
}
