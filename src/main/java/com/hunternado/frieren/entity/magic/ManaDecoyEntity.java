package com.hunternado.frieren.entity.magic;

import com.hunternado.frieren.entity.ai.IdleLookGoal;
import com.hunternado.frieren.registry.ModEntities;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Land's doppelgänger decoy: stands still, soaks attention, then fades. */
public class ManaDecoyEntity extends PathfinderMob {
    private @Nullable UUID ownerId;
    private int lifetime = 200;

    public ManaDecoyEntity(EntityType<? extends ManaDecoyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static @Nullable ManaDecoyEntity spawn(ServerLevel level, Player owner, int lifetime) {
        ManaDecoyEntity decoy = ModEntities.MANA_DECOY.get().create(level, EntitySpawnReason.TRIGGERED);
        if (decoy == null) {
            return null;
        }
        decoy.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        decoy.ownerId = owner.getUUID();
        decoy.lifetime = lifetime;
        decoy.setCustomName(owner.getDisplayName());
        level.addFreshEntity(decoy);
        return decoy;
    }

    public boolean isOwnedBy(Entity entity) {
        return ownerId != null && ownerId.equals(entity.getUUID());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleLookGoal(this, 16.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel level && --lifetime <= 0) {
            SpellFx.burst(level, this.position().add(0.0D, 1.0D, 0.0D), 0x9FD8FF, 0.8F);
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Lifetime", lifetime);
        if (ownerId != null) {
            output.putString("Owner", ownerId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        lifetime = input.getIntOr("Lifetime", 0);
        String owner = input.getStringOr("Owner", "");
        try {
            ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            ownerId = null;
        }
    }
}
