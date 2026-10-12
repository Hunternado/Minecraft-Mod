package com.hunternado.frieren.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Finding somewhere a humanoid can actually stand, so spawned entities never end up inside blocks. */
public final class Ground {
    private Ground() {}

    /** Scans a few blocks up and down for solid ground with two free blocks above; null if none or not loaded. */
    public static @Nullable Vec3 standable(ServerLevel level, Vec3 at) {
        BlockPos base = BlockPos.containing(at);
        for (int dy = 4; dy >= -6; dy--) {
            BlockPos feet = base.offset(0, dy, 0);
            if (!level.getChunkSource().hasChunk(feet.getX() >> 4, feet.getZ() >> 4)) {
                return null;
            }
            BlockPos below = feet.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && level.getFluidState(feet).isEmpty()) {
                return new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
            }
        }
        return null;
    }
}
