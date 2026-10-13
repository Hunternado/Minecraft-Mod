package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.registry.ModBlocks;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The spell to find glittering stones: feels the ores within a radius and tells the caster how many of each lie
 * hidden, with the distance and direction to the nearest of every kind; the nearest ones flash if in sight.
 * Params: {@code radius}.
 */
public class OreSenseBehavior implements SpellBehavior {
    private record Kind(String key, TagKey<Block> tag, int color) {}

    private static final Kind[] KINDS = {
        new Kind("coal", ores("coal_ores"), 0x3A3A3A), new Kind("copper", BlockTags.COPPER_ORES, 0xE07A4A),
        new Kind("iron", BlockTags.IRON_ORES, 0xD8B8A0), new Kind("gold", BlockTags.GOLD_ORES, 0xF3D24A),
        new Kind("redstone", ores("redstone_ores"), 0xE0302A), new Kind("lapis", ores("lapis_ores"), 0x3A5AD8),
        new Kind("diamond", ores("diamond_ores"), 0x6AE8E0), new Kind("emerald", ores("emerald_ores"), 0x3AD86A),
    };

    /** Vanilla ships these block tags as data only; BlockTags has constants for just iron, gold and copper. */
    private static TagKey<Block> ores(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(name));
    }

    @Override
    public boolean cast(SpellContext ctx) {
        int radius = (int) ctx.param("radius", 12.0D);
        BlockPos center = ctx.caster().blockPosition();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, BlockPos> nearest = new LinkedHashMap<>();
        Map<String, Integer> colors = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > (double) radius * radius) {
                continue;
            }
            BlockState state = ctx.level().getBlockState(pos);
            String key = null;
            int color = 0;
            if (state.is(ModBlocks.MANA_CRYSTAL_ORE.get()) || state.is(ModBlocks.DEEPSLATE_MANA_CRYSTAL_ORE.get())) {
                key = "mana_crystal";
                color = 0x4FA8FF;
            } else {
                for (Kind kind : KINDS) {
                    if (state.is(kind.tag())) {
                        key = kind.key();
                        color = kind.color();
                        break;
                    }
                }
            }
            if (key == null) {
                continue;
            }
            counts.merge(key, 1, Integer::sum);
            colors.put(key, color);
            BlockPos best = nearest.get(key);
            if (best == null || pos.distSqr(center) < best.distSqr(center)) {
                nearest.put(key, pos.immutable());
            }
        }
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0, 0.2, 0), ctx.color(), radius * 0.5F);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.UTILITY.get(), 0.7F, 0.7F);
        if (counts.isEmpty()) {
            Messages.chat(ctx.caster(), Component.translatable("message.frieren.ore_sense_nothing", radius).withStyle(ChatFormatting.GRAY));
            return true;
        }
        Messages.chat(ctx.caster(), Component.translatable("message.frieren.ore_sense_header", radius).withStyle(ChatFormatting.AQUA));
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            BlockPos pos = nearest.get(entry.getKey());
            MutableComponent line = Component.translatable("message.frieren.ore_sense_line",
                Component.translatable("message.frieren.ore." + entry.getKey()), entry.getValue(),
                (int) Math.round(Math.sqrt(pos.distSqr(center))), direction(center, pos));
            Messages.chat(ctx.caster(), line.withStyle(ChatFormatting.GRAY));
            SpellFx.burst(ctx.level(), Vec3.atCenterOf(pos), colors.get(entry.getKey()), 0.6F);
        }
        return true;
    }

    /** "12 below, north-east" style direction words, built from translatable parts. */
    private static Component direction(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        String ns = dz < -1 ? "north" : (dz > 1 ? "south" : "");
        String ew = dx > 1 ? "east" : (dx < -1 ? "west" : "");
        String horizontal = ns.isEmpty() && ew.isEmpty() ? "here" : (ns.isEmpty() ? ew : (ew.isEmpty() ? ns : ns + "_" + ew));
        String vertical = dy < -1 ? "below" : (dy > 1 ? "above" : "level");
        return Component.translatable("message.frieren.dir." + vertical, Math.abs(dy),
            Component.translatable("message.frieren.dir." + horizontal));
    }
}
