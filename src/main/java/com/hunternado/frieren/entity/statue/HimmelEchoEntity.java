package com.hunternado.frieren.entity.statue;

import com.hunternado.frieren.spell.SpellFx;
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
 * A fleeting echo of Himmel that appears beside his statue the moment someone polishes it to a full shine: he looks
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

    /** Appears next to {@code statue}, facing {@code polisher}. */
    public static void appear(ServerLevel level, HeroStatueEntity statue, Player polisher, EntityType<HimmelEchoEntity> type) {
        HimmelEchoEntity echo = type.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (echo == null) {
            return;
        }
        Vec3 toPlayer = polisher.position().subtract(statue.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 side = toPlayer.lengthSqr() > 1.0E-4D ? toPlayer.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 at = statue.position().add(side.scale(1.8D)).add(new Vec3(-side.z, 0.0D, side.x).scale(1.2D));
        float yaw = (float) (Math.atan2(polisher.getZ() - at.z, polisher.getX() - at.x) * (180.0D / Math.PI)) - 90.0F;
        echo.snapTo(at.x, polisher.getY(), at.z, yaw, 0.0F);
        echo.setYHeadRot(yaw);
        echo.setYBodyRot(yaw);
        level.addFreshEntity(echo);
        SpellFx.petals(level, at, 0x8FB8FF, 1.5F);
        int line = level.getRandom().nextInt(LINES);
        Messages.nearby(level, at, 16.0D, Component.translatable("message.frieren.himmel_echo." + line)
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
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
