package com.hunternado.frieren.spell.behavior;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.data.MagicData;
import com.hunternado.frieren.registry.ModSounds;
import com.hunternado.frieren.spell.SpellBehavior;
import com.hunternado.frieren.spell.SpellContext;
import com.hunternado.frieren.spell.SpellFx;
import com.hunternado.frieren.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/**
 * The spell to find something lost. Points to the caster's last death position in this dimension;
 * otherwise to the nearest ruin tagged {@code #frieren:seekable}. Params: {@code search_radius} (chunks).
 */
public class SeekBehavior implements SpellBehavior {
    public static final TagKey<Structure> SEEKABLE = TagKey.create(Registries.STRUCTURE, FrierenIds.id("seekable"));

    @Override
    public boolean cast(SpellContext ctx) {
        MagicData data = ctx.data();
        BlockPos target = null;
        String key;
        if (data.lastDeathPos() != null && ctx.level().dimension().identifier().toString().equals(data.lastDeathDimension())) {
            target = data.lastDeathPos();
            key = "message.frieren.seek_death";
        } else {
            target = ctx.level().findNearestMapStructure(SEEKABLE, ctx.caster().blockPosition(), (int) ctx.param("search_radius", 64.0D), false);
            key = "message.frieren.seek_ruin";
        }
        if (target == null) {
            Messages.actionBar(ctx.caster(), Component.translatable("message.frieren.seek_nothing"));
            return false;
        }
        Vec3 from = ctx.caster().position();
        Vec3 delta = Vec3.atCenterOf(target).subtract(from);
        int distance = (int) Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        Messages.chat(ctx.caster(), Component.translatable(key, Component.translatable(compass(delta)), distance));
        Vec3 direction = new Vec3(delta.x, 0.0D, delta.z).normalize();
        SpellFx.beam(ctx.level(), from.add(0, 1.2D, 0), from.add(0, 1.2D, 0).add(direction.scale(6.0D)), ctx.color());
        SpellFx.sound(ctx.level(), from, ModSounds.UTILITY.get(), 0.8F, 0.9F);
        return true;
    }

    private static String compass(Vec3 delta) {
        double angle = Math.toDegrees(Math.atan2(delta.x, -delta.z));
        String[] names = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        int index = (int) Math.round(((angle % 360) + 360) % 360 / 45.0D) % 8;
        return "direction.frieren." + names[index];
    }
}
