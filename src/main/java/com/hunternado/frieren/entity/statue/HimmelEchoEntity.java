package com.hunternado.frieren.entity.statue;

import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Ground;
import com.hunternado.frieren.util.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A fleeting echo of Himmel that appears near his statue the moment someone polishes it to a full shine: he looks
 * at whoever did it, says a few words and fades away. Purely sentimental, untouchable, and never saved.
 */
public class HimmelEchoEntity extends Mob {
    public static final int LIFETIME = 160;
    private static final int LINES = 4;

    public HimmelEchoEntity(EntityType<? extends HimmelEchoEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D);
    }

    /**
     * Appears on open ground between {@code polisher} and the statue, just off their line of sight, facing them.
     * Never inside the plinth: each candidate spot is checked for floor and headroom, nearest the polisher's height
     * first.
     */
    public static void appear(ServerLevel level, HeroStatueEntity statue, Player polisher, EntityType<HimmelEchoEntity> type) {
        HimmelEchoEntity echo = type.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (echo == null) {
            return;
        }
        Vec3 at = spotNear(level, statue, polisher);
        float yaw = (float) (Math.atan2(polisher.getZ() - at.z, polisher.getX() - at.x) * (180.0D / Math.PI)) - 90.0F;
        echo.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        echo.setYHeadRot(yaw);
        echo.setYBodyRot(yaw);
        level.addFreshEntity(echo);
        SpellFx.petals(level, at, 0x8FB8FF, 1.5F);
        int line = level.getRandom().nextInt(LINES);
        Messages.nearby(level, at, 16.0D, Component.translatable("message.frieren.himmel_echo." + line)
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
    }

    private static Vec3 spotNear(ServerLevel level, HeroStatueEntity statue, Player polisher) {
        Vec3 toStatue = statue.position().subtract(polisher.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 forward = toStatue.lengthSqr() > 1.0E-4D ? toStatue.normalize() : new Vec3(0.0D, 0.0D, -1.0D);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double[][] offsets = {{2.0D, 1.2D}, {2.0D, -1.2D}, {1.0D, 1.6D}, {1.0D, -1.6D}, {0.0D, 1.6D}, {0.0D, -1.6D}};
        Vec3 fallback = null;
        for (double[] offset : offsets) {
            Vec3 candidate = polisher.position().add(forward.scale(offset[0])).add(side.scale(offset[1]));
            if (candidate.multiply(1.0D, 0.0D, 1.0D).distanceToSqr(statue.position().multiply(1.0D, 0.0D, 1.0D)) < 2.25D) {
                continue; // too close to stand beside the statue rather than in it
            }
            Vec3 ground = Ground.standable(level, candidate);
            if (ground == null) {
                continue;
            }
            if (Math.abs(ground.y - polisher.getY()) <= 1.0D) {
                return ground;
            }
            if (fallback == null) {
                fallback = ground;
            }
        }
        return fallback != null ? fallback : polisher.position().add(side.scale(1.2D));
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level() instanceof ServerLevel level && this.tickCount >= LIFETIME) {
            SpellFx.petals(level, this.position().add(0.0D, 1.0D, 0.0D), 0x8FB8FF, 1.0F);
            this.discard();
        }
    }

    /** 1 while fully present, fading in over the first second and out over the last two. */
    public float presence(float partialTicks) {
        float age = this.tickCount + partialTicks;
        return Math.max(0.0F, Math.min(1.0F, Math.min(age / 20.0F, (LIFETIME - age) / 40.0F)));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.discard();
    }
}
