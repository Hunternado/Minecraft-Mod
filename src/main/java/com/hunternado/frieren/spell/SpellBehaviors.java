package com.hunternado.frieren.spell;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.spell.behavior.ArrowRainBehavior;
import com.hunternado.frieren.spell.behavior.BarrageBehavior;
import com.hunternado.frieren.spell.behavior.BarrierBehavior;
import com.hunternado.frieren.spell.behavior.BeamBehavior;
import com.hunternado.frieren.spell.behavior.DecoyBehavior;
import com.hunternado.frieren.spell.behavior.DomeBehavior;
import com.hunternado.frieren.spell.behavior.FlameWaveBehavior;
import com.hunternado.frieren.spell.behavior.FlightBehavior;
import com.hunternado.frieren.spell.behavior.FlowerFieldBehavior;
import com.hunternado.frieren.spell.behavior.GatherBehavior;
import com.hunternado.frieren.spell.behavior.GoldCurseBehavior;
import com.hunternado.frieren.spell.behavior.HealBehavior;
import com.hunternado.frieren.spell.behavior.LightOrbBehavior;
import com.hunternado.frieren.spell.behavior.LightningBehavior;
import com.hunternado.frieren.spell.behavior.MendBehavior;
import com.hunternado.frieren.spell.behavior.MimicDetectionBehavior;
import com.hunternado.frieren.spell.behavior.ObedienceBehavior;
import com.hunternado.frieren.spell.behavior.PurifyBehavior;
import com.hunternado.frieren.spell.behavior.RustRemovalBehavior;
import com.hunternado.frieren.spell.behavior.SeekBehavior;
import com.hunternado.frieren.spell.behavior.SlashBehavior;
import com.hunternado.frieren.spell.behavior.VortexBehavior;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of code-backed spell behaviors, keyed by the id used in spell JSON files. Addons may call
 * {@link #register} during mod construction to add new behaviors that datapacks can then use.
 */
public final class SpellBehaviors {
    private static final Map<Identifier, SpellBehavior> BEHAVIORS = new LinkedHashMap<>();

    private SpellBehaviors() {}

    public static synchronized void bootstrap() {
        if (!BEHAVIORS.isEmpty()) {
            return;
        }
        register(FrierenIds.id("beam"), new BeamBehavior());
        register(FrierenIds.id("barrage"), new BarrageBehavior());
        register(FrierenIds.id("flame_wave"), new FlameWaveBehavior());
        register(FrierenIds.id("lightning"), new LightningBehavior());
        register(FrierenIds.id("vortex"), new VortexBehavior());
        register(FrierenIds.id("arrow_rain"), new ArrowRainBehavior());
        register(FrierenIds.id("slash"), new SlashBehavior());
        register(FrierenIds.id("gold_curse"), new GoldCurseBehavior());
        register(FrierenIds.id("barrier"), new BarrierBehavior());
        register(FrierenIds.id("dome"), new DomeBehavior());
        register(FrierenIds.id("heal"), new HealBehavior());
        register(FrierenIds.id("purify"), new PurifyBehavior());
        register(FrierenIds.id("flight"), new FlightBehavior());
        register(FrierenIds.id("flower_field"), new FlowerFieldBehavior());
        register(FrierenIds.id("mimic_detection"), new MimicDetectionBehavior());
        register(FrierenIds.id("mend"), new MendBehavior());
        register(FrierenIds.id("rust_removal"), new RustRemovalBehavior());
        register(FrierenIds.id("seek"), new SeekBehavior());
        register(FrierenIds.id("light_orb"), new LightOrbBehavior());
        register(FrierenIds.id("gather"), new GatherBehavior());
        register(FrierenIds.id("obedience"), new ObedienceBehavior());
        register(FrierenIds.id("decoy"), new DecoyBehavior());
    }

    public static synchronized void register(Identifier id, SpellBehavior behavior) {
        if (BEHAVIORS.putIfAbsent(id, behavior) != null) {
            throw new IllegalStateException("Duplicate spell behavior " + id);
        }
    }

    public static @Nullable SpellBehavior get(Identifier id) {
        return BEHAVIORS.get(id);
    }

    public static int count() {
        return BEHAVIORS.size();
    }

    public static Iterable<SpellBehavior> all() {
        return BEHAVIORS.values();
    }
}
