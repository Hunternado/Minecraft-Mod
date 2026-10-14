package com.hunternado.frieren.spell;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.spell.behavior.AnalysisBehavior;
import com.hunternado.frieren.spell.behavior.AnnihilationBehavior;
import com.hunternado.frieren.spell.behavior.ArrowRainBehavior;
import com.hunternado.frieren.spell.behavior.BarrageBehavior;
import com.hunternado.frieren.spell.behavior.BarrierBehavior;
import com.hunternado.frieren.spell.behavior.BeamBehavior;
import com.hunternado.frieren.spell.behavior.BindBehavior;
import com.hunternado.frieren.spell.behavior.BlackSunBehavior;
import com.hunternado.frieren.spell.behavior.BloodBladesBehavior;
import com.hunternado.frieren.spell.behavior.BridgeBehavior;
import com.hunternado.frieren.spell.behavior.BuffBehavior;
import com.hunternado.frieren.spell.behavior.ClearWeatherBehavior;
import com.hunternado.frieren.spell.behavior.CookBehavior;
import com.hunternado.frieren.spell.behavior.DashBehavior;
import com.hunternado.frieren.spell.behavior.DeceptionBehavior;
import com.hunternado.frieren.spell.behavior.DecoyBehavior;
import com.hunternado.frieren.spell.behavior.DomeBehavior;
import com.hunternado.frieren.spell.behavior.DominionBehavior;
import com.hunternado.frieren.spell.behavior.EarthSpikesBehavior;
import com.hunternado.frieren.spell.behavior.FlameWaveBehavior;
import com.hunternado.frieren.spell.behavior.FlightBehavior;
import com.hunternado.frieren.spell.behavior.FlowerFieldBehavior;
import com.hunternado.frieren.spell.behavior.GatherBehavior;
import com.hunternado.frieren.spell.behavior.GoldCurseBehavior;
import com.hunternado.frieren.spell.behavior.GolemBehavior;
import com.hunternado.frieren.spell.behavior.GrowthBehavior;
import com.hunternado.frieren.spell.behavior.HarvestBehavior;
import com.hunternado.frieren.spell.behavior.HealBehavior;
import com.hunternado.frieren.spell.behavior.IceShardsBehavior;
import com.hunternado.frieren.spell.behavior.LightOrbBehavior;
import com.hunternado.frieren.spell.behavior.LightningBehavior;
import com.hunternado.frieren.spell.behavior.ManaReleaseBehavior;
import com.hunternado.frieren.spell.behavior.MendBehavior;
import com.hunternado.frieren.spell.behavior.MimicDetectionBehavior;
import com.hunternado.frieren.spell.behavior.MiracleBehavior;
import com.hunternado.frieren.spell.behavior.ObedienceBehavior;
import com.hunternado.frieren.spell.behavior.OreSenseBehavior;
import com.hunternado.frieren.spell.behavior.OriginalBehavior;
import com.hunternado.frieren.spell.behavior.PocketSpaceBehavior;
import com.hunternado.frieren.spell.behavior.PurifyBehavior;
import com.hunternado.frieren.spell.behavior.RecallBehavior;
import com.hunternado.frieren.spell.behavior.ReflectBehavior;
import com.hunternado.frieren.spell.behavior.RustRemovalBehavior;
import com.hunternado.frieren.spell.behavior.SanctuaryBehavior;
import com.hunternado.frieren.spell.behavior.SeekBehavior;
import com.hunternado.frieren.spell.behavior.SlashBehavior;
import com.hunternado.frieren.spell.behavior.SmeltBehavior;
import com.hunternado.frieren.spell.behavior.SovereignWardBehavior;
import com.hunternado.frieren.spell.behavior.SweetenBehavior;
import com.hunternado.frieren.spell.behavior.TreeFellBehavior;
import com.hunternado.frieren.spell.behavior.TunnelBehavior;
import com.hunternado.frieren.spell.behavior.VolleyBehavior;
import com.hunternado.frieren.spell.behavior.VortexBehavior;
import com.hunternado.frieren.spell.behavior.WaterWhipBehavior;
import com.hunternado.frieren.spell.behavior.WireSnareBehavior;
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
        register(FrierenIds.id("bind"), new BindBehavior());
        register(FrierenIds.id("ice_shards"), new IceShardsBehavior());
        register(FrierenIds.id("earth_spikes"), new EarthSpikesBehavior());
        register(FrierenIds.id("water_whip"), new WaterWhipBehavior());
        register(FrierenIds.id("mana_release"), new ManaReleaseBehavior());
        register(FrierenIds.id("blood_blades"), new BloodBladesBehavior());
        register(FrierenIds.id("wire_snare"), new WireSnareBehavior());
        register(FrierenIds.id("reflect"), new ReflectBehavior());
        register(FrierenIds.id("sanctuary"), new SanctuaryBehavior());
        register(FrierenIds.id("dash"), new DashBehavior());
        register(FrierenIds.id("harvest"), new HarvestBehavior());
        register(FrierenIds.id("cook"), new CookBehavior());
        register(FrierenIds.id("sweeten"), new SweetenBehavior());
        register(FrierenIds.id("clear_weather"), new ClearWeatherBehavior());
        register(FrierenIds.id("golem"), new GolemBehavior());
        register(FrierenIds.id("tree_fell"), new TreeFellBehavior());
        register(FrierenIds.id("growth"), new GrowthBehavior());
        register(FrierenIds.id("ore_sense"), new OreSenseBehavior());
        register(FrierenIds.id("tunnel"), new TunnelBehavior());
        register(FrierenIds.id("recall"), new RecallBehavior());
        register(FrierenIds.id("bridge"), new BridgeBehavior());
        register(FrierenIds.id("buff"), new BuffBehavior());
        register(FrierenIds.id("smelt"), new SmeltBehavior());
        register(FrierenIds.id("pocket_space"), new PocketSpaceBehavior());
        register(FrierenIds.id("volley"), new VolleyBehavior());
        register(FrierenIds.id("original"), new OriginalBehavior());
        register(FrierenIds.id("deception"), new DeceptionBehavior());
        register(FrierenIds.id("miracle"), new MiracleBehavior());
        register(FrierenIds.id("analysis"), new AnalysisBehavior());
        register(FrierenIds.id("annihilation"), new AnnihilationBehavior());
        register(FrierenIds.id("black_sun"), new BlackSunBehavior());
        register(FrierenIds.id("dominion"), new DominionBehavior());
        register(FrierenIds.id("sovereign_ward"), new SovereignWardBehavior());
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
