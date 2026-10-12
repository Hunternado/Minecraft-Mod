package com.hunternado.frieren.command;

import com.hunternado.frieren.entity.fx.BarrierShellEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /frieren fx <pos>}: lays out every magic visual around a point (for screenshots and eyeballing art).
 * The layout faces a viewer standing north of {@code pos} and looking south.
 */
final class FxSampler {
    private static final int LIFE = 20 * 60;

    private FxSampler() {}

    static void spawn(ServerLevel level, Vec3 pos) {
        // Ground circles, one per style.
        int[] colors = {0x8FD3FF, 0xFFE9A8, 0xD0304A, 0xF6A6D8};
        byte[] styles = {MagicCircleEntity.STYLE_ARCANE, MagicCircleEntity.STYLE_HOLY, MagicCircleEntity.STYLE_DEMON, MagicCircleEntity.STYLE_FLORA};
        for (int i = 0; i < 4; i++) {
            SpellFx.groundCircle(level, pos.add(-6.0D + i * 4.0D, 0.0D, 6.0D), styles[i], colors[i], 1.7F, LIFE);
        }
        // Standing circles facing the viewer (yaw 180 looks north).
        MagicCircleEntity.standing(level, pos.add(-3.0D, 2.2D, 2.0D), 180.0F, 0.0F, MagicCircleEntity.STYLE_ARCANE, 0x8FD3FF, 1.2F, LIFE);
        MagicCircleEntity.standing(level, pos.add(3.0D, 2.2D, 2.0D), 180.0F, 0.0F, MagicCircleEntity.STYLE_DEMON, 0xD0304A, 1.2F, LIFE);
        // Beams: Zoltraak across the scene, a demon beam, lightning from above.
        SpellBeamEntity.fire(level, pos.add(-8.0D, 1.4D, 9.0D), pos.add(8.0D, 1.4D, 9.0D), 0x9FD8FF, 0.4F, LIFE, SpellBeamEntity.STYLE_ZOLTRAAK);
        SpellBeamEntity.fire(level, pos.add(-8.0D, 3.4D, 10.0D), pos.add(8.0D, 2.4D, 10.0D), 0xC0203A, 0.35F, LIFE, SpellBeamEntity.STYLE_DEMON);
        SpellBeamEntity.fire(level, pos.add(0.0D, 12.0D, 12.0D), pos.add(0.0D, 0.0D, 12.0D), 0xFFF2A0, 0.5F, LIFE, SpellBeamEntity.STYLE_LIGHTNING);
        // A barrier shell around a still Frieren.
        Mob frieren = ModEntities.FRIEREN.get().create(level, EntitySpawnReason.COMMAND);
        if (frieren != null) {
            frieren.snapTo(pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
            frieren.setYHeadRot(180.0F);
            frieren.setNoAi(true);
            level.addFreshEntity(frieren);
            BarrierShellEntity.flash(level, frieren, 0x9FE6FF, LIFE);
        }
        particles(level, pos);
    }

    /** Short-lived particle families; re-run for every screenshot. */
    static void particles(ServerLevel level, Vec3 pos) {
        SpellFx.burst(level, pos.add(-6.0D, 1.0D, 4.0D), 0x8FD3FF, 1.2F);
        SpellFx.petals(level, pos.add(6.0D, 0.5D, 6.0D), 0xF6A6D8, 2.0F);
        SpellFx.frost(level, pos.add(-2.0D, 0.5D, 6.0D), 0xBFEFFF, 1.5F);
        SpellFx.wisps(level, pos.add(2.0D, 0.5D, 6.0D), 0xD0304A, 1.5F);
        SpellFx.spiral(level, pos.add(-4.0D, 0.0D, 3.0D), 0xFFE9A8, 3.0F);
        SpellFx.ring(level, pos.add(4.0D, 0.2D, 3.0D), 0x8FD3FF, 1.5F);
    }
}
