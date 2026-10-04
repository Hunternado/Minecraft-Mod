package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.quest.QuestManager;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The spell to tell whether a treasure chest is a mimic — with the canonical 99% accuracy. Each mimic has
 * a 1% chance to be declared "definitely not a mimic". Params: {@code radius}.
 */
public class MimicDetectionBehavior implements SpellBehavior {
    @Override
    public boolean cast(SpellContext ctx) {
        double radius = ctx.param("radius", 20.0D);
        List<Entity> mimics = ctx.level().getEntities(ctx.caster(), new AABB(ctx.caster().blockPosition()).inflate(radius),
            entity -> entity instanceof MimicEntity);
        int revealed = 0;
        boolean falseNegative = false;
        for (Entity entity : mimics) {
            MimicEntity mimic = (MimicEntity) entity;
            if (ctx.level().getRandom().nextInt(100) == 0) {
                falseNegative = true;
                continue;
            }
            mimic.reveal(ctx.caster());
            SpellFx.burst(ctx.level(), mimic.position().add(0, 0.5D, 0), ctx.color(), 0.8F);
            revealed++;
        }
        MagicData data = ctx.data();
        if (revealed > 0) {
            ctx.markHit();
            for (int i = 0; i < revealed; i++) {
                data.incrementStat("mimics_revealed");
            }
            QuestManager.onStat(ctx.caster(), data, "mimics_revealed");
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.mimics_found", revealed));
        } else if (falseNegative) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.mimics_false_negative"));
        } else {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.mimics_none"));
        }
        SpellFx.ring(ctx.level(), ctx.caster().position().add(0, 0.1D, 0), ctx.color(), (float) Math.min(radius, 8.0D));
        SpellFx.sound(ctx.level(), ctx.caster().position(), ModSounds.UTILITY.get(), 0.8F, 1.5F);
        return true;
    }
}
