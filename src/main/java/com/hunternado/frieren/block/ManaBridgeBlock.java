package com.hunternado.frieren.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plank of solid light laid by the bridge spell: a thin walkable plate flush with the top of its block, so a bridge
 * of them is level with the ground it leaves from. Neighbouring planks hide their shared faces like glass. Schedules
 * its own removal when placed and shatters quietly when it goes.
 */
public class ManaBridgeBlock extends TransparentBlock {
    /** One minute. */
    public static final int LIFETIME_TICKS = 1200;
    private static final VoxelShape PLATE = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public ManaBridgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PLATE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            // a second of jitter, so a bridge crumbles plank by plank instead of vanishing in one tick
            level.scheduleTick(pos, this, LIFETIME_TICKS + level.getRandom().nextInt(20));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.destroyBlock(pos, false);
    }
}
