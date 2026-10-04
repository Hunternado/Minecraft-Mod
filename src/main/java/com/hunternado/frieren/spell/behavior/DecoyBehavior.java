package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.entity.magic.ManaDecoyEntity;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Land's doppelgänger: leaves a mana decoy that hostile mobs target for {@code duration} ticks while the
 * caster briefly fades from sight. Params: {@code duration}, {@code radius}.
 */
public class DecoyBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        ManaDecoyEntity decoy = ManaDecoyEntity.spawn(ctx.level(), ctx.caster(), (int) ctx.param("duration", 200.0D));
        if (decoy == null) {
            return false;
        }
        double radius = ctx.param("radius", 16.0D);
        for (Entity entity : ctx.level().getEntities(ctx.caster(), new AABB(ctx.caster().blockPosition()).inflate(radius),
            e -> e instanceof Mob)) {
            Mob mob = (Mob) entity;
            if (mob.getTarget() == ctx.caster()) {
                mob.setTarget(decoy);
                ctx.markHit();
            }
        }
        ctx.caster().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
        SpellFx.burst(ctx.level(), decoy.position().add(0, 1.0D, 0), ctx.color(), 1.0F);
        SpellFx.sound(ctx.level(), decoy.position(), ModSounds.TELEPORT.get(), 0.8F, 1.3F);
        return true;
    }
}
