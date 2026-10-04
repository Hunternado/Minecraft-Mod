package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.registry.ModBlocks;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flamme's spell to create a field of flowers (Frieren's favourite). Blooms flowers on open grass around
 * the caster and leaves a soothing aura that regenerates allies. Placing flowers on empty air above grass
 * is never destructive, so it ignores the griefing setting. Params: {@code radius}, {@code density}.
 */
public class FlowerFieldBehavior implements SpellBehavior {
    private static final Block[] FLOWERS = {
        Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.ALLIUM, Blocks.BLUE_ORCHID
    };

    @Override
    public boolean cast(SpellContext ctx) {
        int radius = (int) ctx.param("radius", 6.0D);
        double density = ctx.param("density", 0.35D);
        RandomSource random = ctx.level().getRandom();
        BlockPos origin = ctx.caster().blockPosition();
        int bloomed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextDouble() > density) {
                    continue;
                }
                for (int dy = 3; dy >= -3; dy--) {
                    BlockPos ground = origin.offset(dx, dy, dz);
                    BlockPos above = ground.above();
                    if (ctx.level().getBlockState(ground).is(Blocks.GRASS_BLOCK) && ctx.level().isEmptyBlock(above)) {
                        Block flower = random.nextInt(40) == 0 ? ModBlocks.BLUE_MOON_WEED.get() : FLOWERS[random.nextInt(FLOWERS.length)];
                        BlockState state = flower.defaultBlockState();
                        if (state.canSurvive(ctx.level(), above)) {
                            ctx.level().setBlock(above, state, 3);
                            bloomed++;
                        }
                        break;
                    }
                }
            }
        }
        SpellAreaEntity.spawn(ctx.level(), ctx.caster(), ctx.caster().position(), SpellAreaEntity.Mode.FLOWER_FIELD,
            radius, (int) ctx.param("duration", 200.0D), 0.0F, ctx.color());
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0, 0.2D, 0), ctx.color(), radius);
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.FLOWER_BLOOM.get(), 1.0F, 1.0F);
        if (bloomed > 0) {
            ctx.markHit();
        }
        return true;
    }
}
