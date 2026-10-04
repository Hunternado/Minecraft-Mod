package com.hunternado.frieren.entity.boss;

import com.hunternado.frieren.combat.ModDamageTypes;
import com.hunternado.frieren.config.FrierenConfig;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.ai.RangedSpellGoal;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellDamage;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.spell.SpellTargeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A mirror image of an examinee created by Spiegel. It copies the source's vitality, armour and rank (not
 * their items, so nothing can be duplicated) and casts a mirrored Zoltraak back at them.
 */
public class MirrorReplicaEntity extends Monster implements RangedSpellGoal.Caster {
    private int rankPower;

    public MirrorReplicaEntity(EntityType<? extends MirrorReplicaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.ATTACK_DAMAGE, 4.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedSpellGoal(this, this, 1.0D, 3.0D, 14.0D, 60));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1D, false));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Called by Spiegel right after creation. */
    public void copyFrom(ServerPlayer source, SpiegelEntity spiegel) {
        MagicData data = MagicData.get(source);
        rankPower = data != null ? data.rank().ordinal() : 0;
        setBase(Attributes.MAX_HEALTH, Math.max(10.0D, source.getMaxHealth()));
        setBase(Attributes.ARMOR, source.getArmorValue());
        setBase(Attributes.ATTACK_DAMAGE, 4.0D + rankPower * 0.75D);
        this.setHealth(this.getMaxHealth());
        this.setCustomName(Component.translatable("entity.frieren.mirror_replica.of", source.getDisplayName()));
        this.setTarget(source);
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void castAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = this.getEyePosition();
        Vec3 to = target.getEyePosition();
        List<SpellTargeting.EntityHit> hits = SpellTargeting.entitiesOnLine(level, this, from, to, 0.3D,
            entity -> entity == target);
        float damage = (float) ((4.0D + rankPower) * FrierenConfig.bossDamageMultiplier());
        if (!hits.isEmpty()) {
            SpellDamage.hurt(level, this, this, target, damage, ModDamageTypes.ZOLTRAAK, 1.0F, false, null);
        }
        SpellFx.beam(level, from, to, 0xDDEEFF);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ZOLTRAAK.get(), SoundSource.HOSTILE, 0.8F, 1.3F);
    }
}
