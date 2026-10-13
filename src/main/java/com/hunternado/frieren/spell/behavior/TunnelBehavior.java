package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/**
 * The spell to carve a tunnel: a 3x3 passage opens in the direction the caster faces (straight up and down
 * included). Earth yields as it would to an iron pickaxe, so stone and ores drop what they would; block entities,
 * fluids, unbreakable and obsidian-hard blocks are left alone, and spawn protection and break events (claim mods)
 * apply to every block. Params: {@code depth}.
 */
public class TunnelBehavior implements SpellBehavior {
    private static final ItemStack VIRTUAL_PICKAXE = new ItemStack(Items.IRON_PICKAXE);

    @Override
    public boolean cast(SpellContext ctx) {
        ServerPlayer caster = ctx.caster();
        int depth = (int) ctx.param("depth", 6.0D);
        Direction facing = Direction.orderedByNearest(caster)[0];
        boolean vertical = facing.getAxis().isVertical();
        // the passage is centred on the caster's body: their eye block when walking, the column under or over them
        BlockPos start = switch (facing) {
            case DOWN -> caster.blockPosition().below();
            case UP -> caster.blockPosition().above(2);
            default -> BlockPos.containing(caster.getEyePosition()).relative(facing);
        };
        Direction across = vertical ? Direction.EAST : facing.getClockWise();
        Direction along = vertical ? Direction.SOUTH : Direction.UP;
        int carved = 0;
        for (int step = 0; step < depth; step++) {
            BlockPos middle = start.relative(facing, step);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (carve(ctx.level(), caster, middle.relative(across, a).relative(along, b))) {
                        carved++;
                    }
                }
            }
        }
        if (carved == 0) {
            Messages.actionBar(caster, Component.translatable("message.frieren.tunnel_nothing"));
            return false;
        }
        Messages.actionBar(caster, Component.translatable("message.frieren.tunnel_done", carved));
        SpellFx.cone(ctx.level(), caster.getEyePosition(), Vec3.atLowerCornerOf(facing.getUnitVec3i()), ctx.color(), depth, 1);
        SpellFx.sound(ctx.level(), caster.position(), SoundEvents.DEEPSLATE_BREAK, 1.2F, 0.6F);
        return true;
    }

    private static boolean carve(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 30.0F || !level.mayInteract(caster, pos)
                || ForgeHooks.onBlockBreakEvent(level, caster.gameMode(), caster, pos) == -1) {
            return false;
        }
        for (ItemStack drop : Block.getDrops(state, level, pos, null, caster, VIRTUAL_PICKAXE)) {
            Block.popResource(level, pos, drop);
        }
        return level.destroyBlock(pos, false, caster);
    }
}
