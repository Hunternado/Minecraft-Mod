package com.hunternado.frieren.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Supplier;

/**
 * Spawn egg that resolves its entity type lazily, so item registration never depends on the order in
 * which Forge fires registry events.
 */
public class FrierenSpawnEggItem extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> type;

    public FrierenSpawnEggItem(Properties properties, Supplier<? extends EntityType<? extends Mob>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Mob mob = type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (mob == null) {
            return InteractionResult.FAIL;
        }
        mob.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWN_ITEM_USE, null);
        level.addFreshEntity(mob);
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
