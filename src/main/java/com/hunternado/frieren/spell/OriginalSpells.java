package com.hunternado.frieren.spell;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.magic.SpellRarity;
import com.hunternado.frieren.magic.SpellSchool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Original magic: spells Serie creates for one mage, from a recipe the mage asks for at her spell forge. Everything
 * about a spell's cost follows from what it can do, and the same formulas run on the client (the forge's live
 * preview) and the server (which recomputes everything and trusts nothing the client sends):
 * <ul>
 *   <li><b>form</b>: beam, volley of bolts, blast at the aimed point, nova around the caster, ward on the caster,
 *   mending of allies, or binding of foes;</li>
 *   <li><b>element</b>: plain mana, flame (burns), frost (slows and freezes), storm (arcs to more foes), curse
 *   (withers and weakens), holy (double against demons and the undead) or void (cuts through barriers);</li>
 *   <li><b>potency</b> 1-5, and 6, <i>Annihilation</i>: kills anything that is not a boss outright and takes 30% of a
 *   boss's health, at 450 mana before modifiers, a five-second charge that a hit can break, and a 90-second
 *   cooldown;</li>
 *   <li><b>reach</b> 1-3 (range, radius, number of bolts or arcs);</li>
 *   <li><b>casting</b>: instant (potency 1-2 only), incantation, or charge (required for Annihilation);</li>
 *   <li><b>modifiers</b>: piercing, swift (half the cooldown), silent (cast without breaking concealment),
 *   lifesteal, unwavering (cannot be interrupted; never on Annihilation).</li>
 * </ul>
 * A mage can hold {@link #MAX_SPELLS} original spells; asking again for a slot replaces what was there.
 */
public final class OriginalSpells {
    public static final int MAX_SPELLS = 3;
    public static final int MAX_NAME = 24;

    public static final String[] FORMS = {"beam", "volley", "blast", "nova", "ward", "mend", "bind"};
    public static final String[] ELEMENTS = {"mana", "flame", "frost", "storm", "curse", "holy", "void"};
    public static final String[] CASTINGS = {"instant", "incantation", "charge"};
    public static final int[] COLORS = {0xB8E0FF, 0x7FD4FF, 0xFFFFFF, 0xFFD24A, 0xFF7A2A, 0xE04040,
                                        0xFF8AD0, 0xB070FF, 0x6AE07A, 0x40E0D0, 0x9A9AB0, 0x2A1E40};

    public static final int FORM_BEAM = 0;
    public static final int FORM_VOLLEY = 1;
    public static final int FORM_BLAST = 2;
    public static final int FORM_NOVA = 3;
    public static final int FORM_WARD = 4;
    public static final int FORM_MEND = 5;
    public static final int FORM_BIND = 6;

    public static final int EL_MANA = 0;
    public static final int EL_FLAME = 1;
    public static final int EL_FROST = 2;
    public static final int EL_STORM = 3;
    public static final int EL_CURSE = 4;
    public static final int EL_HOLY = 5;
    public static final int EL_VOID = 6;

    public static final int CAST_INSTANT = 0;
    public static final int CAST_INCANT = 1;
    public static final int CAST_CHARGE = 2;

    public static final int MOD_PIERCE = 1;
    public static final int MOD_SWIFT = 2;
    public static final int MOD_SILENT = 4;
    public static final int MOD_LIFESTEAL = 8;
    public static final int MOD_UNWAVERING = 16;
    public static final String[] MODIFIERS = {"pierce", "swift", "silent", "lifesteal", "unwavering"};

    public static final int ANNIHILATION = 6;

    private static final float[] DAMAGE = {0, 6, 10, 15, 22, 32, 40};
    private static final float[] COST = {0, 12, 24, 42, 72, 120, 450};
    private static final int[] COOLDOWN = {0, 20, 40, 80, 160, 300, 1800};
    private static final int[] INCANT = {0, 8, 14, 22, 34, 50, 80};
    private static final int[] CHARGE = {0, 16, 24, 36, 52, 70, 100};

    private OriginalSpells() {}

    // =========================================================================================
    // Recipe
    // =========================================================================================

    /** What the mage asked for. Out-of-range values are clamped by {@link #sanitized()}, never trusted. */
    public record Recipe(int form, int element, int potency, int reach, int casting, int modifiers, int color, String name) {
        public Recipe sanitized() {
            int f = clamp(form, 0, FORMS.length - 1);
            int p = clamp(potency, 1, ANNIHILATION);
            int c = clamp(casting, 0, CASTINGS.length - 1);
            if (p >= ANNIHILATION) {
                c = CAST_CHARGE;                                  // nothing that kills everything comes quickly
            } else if (p >= 3 && c == CAST_INSTANT) {
                c = CAST_INCANT;
            }
            if (f == FORM_WARD || f == FORM_MEND) {
                p = Math.min(p, 5);                               // there is no "annihilating" heal
            }
            String clean = name == null ? "" : name.replaceAll("[\\p{Cntrl}\u00A7]", "").strip();
            if (clean.length() > MAX_NAME) {
                clean = clean.substring(0, MAX_NAME).strip();
            }
            int mods = modifiers & (MOD_PIERCE | MOD_SWIFT | MOD_SILENT | MOD_LIFESTEAL | MOD_UNWAVERING);
            if (p >= ANNIHILATION) {
                mods &= ~MOD_UNWAVERING;                          // a spell that kills anything can always be broken
            }
            return new Recipe(f, clamp(element, 0, ELEMENTS.length - 1), p, clamp(reach, 1, 3), c, mods,
                clamp(color, 0, COLORS.length - 1), clean);
        }

        public boolean has(int modifier) {
            return (modifiers & modifier) != 0;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("form", form);
            tag.putInt("element", element);
            tag.putInt("potency", potency);
            tag.putInt("reach", reach);
            tag.putInt("casting", casting);
            tag.putInt("modifiers", modifiers);
            tag.putInt("color", color);
            tag.putString("name", name);
            return tag;
        }

        public static Recipe load(CompoundTag tag) {
            return new Recipe(tag.getIntOr("form", 0), tag.getIntOr("element", 0), tag.getIntOr("potency", 1), tag.getIntOr("reach", 1),
                tag.getIntOr("casting", 0), tag.getIntOr("modifiers", 0), tag.getIntOr("color", 0), tag.getStringOr("name", "")).sanitized();
        }
    }

    /** Everything that follows from a recipe; shown live at the forge and baked into the spell. */
    public record Stats(float manaCost, int cooldown, int castTime, int chargeTicks, float damage, double range, double radius,
                        int count, int duration, float heal, float absorb, float executeCap, float bossFraction, float fragility,
                        boolean steady) {
        public boolean interruptible() {
            return !steady && (castTime > 0 || chargeTicks > 0);
        }
    }

    public static Stats compute(Recipe raw) {
        Recipe r = raw.sanitized();
        int p = r.potency();
        float reachScale = 1.0F + 0.25F * (r.reach() - 1);
        float damage = DAMAGE[p];
        double range = 0.0D;
        double radius = 0.0D;
        int count = 1;
        int duration = 0;
        float heal = 0.0F;
        float absorb = 0.0F;
        float formCost;
        switch (r.form()) {
            case FORM_VOLLEY -> {
                count = 2 + 2 * r.reach();
                damage *= 0.4F;
                range = 32.0D;
                formCost = 1.2F;
            }
            case FORM_BLAST -> {
                damage *= 0.85F;
                range = 24.0D + 8.0D * r.reach();
                radius = 1.8D + 1.2D * r.reach() + 0.3D * p;
                formCost = 1.3F;
            }
            case FORM_NOVA -> {
                damage *= 0.8F;
                radius = 3.0D + 1.5D * r.reach() + 0.3D * p;
                formCost = 1.2F;
            }
            case FORM_WARD -> {
                absorb = 8.0F * p;
                duration = 200 + 100 * r.reach();
                damage = 0.0F;
                formCost = 0.9F;
            }
            case FORM_MEND -> {
                heal = 4.0F * p * (r.element() == EL_HOLY ? 1.5F : 1.0F);
                radius = 3.0D + 2.0D * r.reach();
                damage = 0.0F;
                formCost = 1.0F;
            }
            case FORM_BIND -> {
                damage *= 0.25F;
                duration = 40 + 20 * p;
                range = 24.0D;
                radius = 2.0D + 1.5D * r.reach();
                formCost = 1.1F;
            }
            default -> {
                range = 24.0D + 12.0D * r.reach();
                formCost = 1.0F;
            }
        }
        if (r.element() == EL_STORM && damage > 0.0F) {
            count = Math.max(count, 1) + r.reach();               // arcs to more foes (beams/blasts) or more bolts
        }
        float cost = (8.0F + COST[p]) * formCost * reachScale;
        if (r.has(MOD_PIERCE)) {
            cost *= 1.15F;
        }
        if (r.has(MOD_SILENT)) {
            cost *= 1.2F;
        }
        if (r.has(MOD_LIFESTEAL)) {
            cost *= 1.25F;
        }
        if (r.has(MOD_SWIFT)) {
            cost *= 1.3F;
        }
        boolean steady = r.has(MOD_UNWAVERING);
        if (steady) {
            cost *= 1.5F;
        }
        int cooldown = Math.round(COOLDOWN[p] * (r.has(MOD_SWIFT) ? 0.5F : 1.0F) * (r.form() == FORM_VOLLEY ? 1.2F : 1.0F));
        int castTime = r.casting() == CAST_INCANT ? Math.round(INCANT[p] * (steady ? 1.3F : 1.0F)) : 0;
        int charge = r.casting() == CAST_CHARGE ? Math.round(CHARGE[p] * (steady ? 1.3F : 1.0F)) : 0;
        float executeCap = p >= ANNIHILATION ? 100000.0F : p == 5 ? 200.0F : 0.0F;
        if (damage <= 0.0F) {
            executeCap = 0.0F;
        }
        float bossFraction = p >= ANNIHILATION && damage > 0.0F ? 0.3F : 0.0F;
        float fragility = p >= ANNIHILATION ? 1.8F : p == 5 ? 1.3F : 1.0F;
        return new Stats(Math.round(cost), cooldown, castTime, charge, damage, range, radius, count, duration, heal, absorb,
            executeCap, bossFraction, fragility, steady);
    }

    // =========================================================================================
    // Building the spell
    // =========================================================================================

    /** The spell's id: unique per player and slot, so two players' spells never collide. */
    public static Identifier idFor(UUID owner, int slot) {
        return FrierenIds.id("original/" + owner.toString().replace("-", "") + "_" + slot);
    }

    public static SpellDefinition build(Identifier id, Recipe raw) {
        Recipe r = raw.sanitized();
        Stats s = compute(r);
        Map<String, Double> params = new HashMap<>();
        params.put("form", (double) r.form());
        params.put("element", (double) r.element());
        params.put("potency", (double) r.potency());
        params.put("reach", (double) r.reach());
        params.put("modifiers", (double) r.modifiers());
        params.put("damage", (double) s.damage());
        params.put("range", s.range());
        params.put("radius", s.radius());
        params.put("count", (double) s.count());
        params.put("duration", (double) s.duration());
        params.put("heal", (double) s.heal());
        params.put("absorb", (double) s.absorb());
        if (s.chargeTicks() > 0) {
            params.put("charge", (double) s.chargeTicks());
            params.put("min_charge", 0.3D);
        }
        if (s.executeCap() > 0.0F) {
            params.put("execute_max_health", (double) s.executeCap());
        }
        if (s.bossFraction() > 0.0F) {
            params.put("boss_fraction", (double) s.bossFraction());
        }
        params.put("fragility", (double) s.fragility());
        if (s.steady()) {
            params.put("steady", 1.0D);
        }
        if (r.has(MOD_PIERCE)) {
            params.put("pierce", 1.0D);
        }
        if (r.has(MOD_SILENT)) {
            params.put("silent", 1.0D);
        }
        params.put("barrier_cost", r.element() == EL_VOID ? 0.15D : 1.0D);
        String name = r.name().isEmpty() ? defaultName(r) : r.name();
        return new SpellDefinition(id, FrierenIds.id("original"), SpellSchool.ORIGINAL, SpellRarity.MYTHIC, s.manaCost(), 0.0F,
            s.cooldown(), s.castTime(), 0, COLORS[r.color()], Map.copyOf(params), name);
    }

    /** "Serie's Flame Beam" style fallback when the mage left the name empty. */
    public static String defaultName(Recipe r) {
        return capital(ELEMENTS[r.element()]) + " " + capital(FORMS[r.form()]);
    }

    private static String capital(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    /** The grimoire description, assembled from the spell's own numbers. */
    public static Component describe(SpellDefinition spell) {
        int form = clamp((int) spell.param("form", 0.0D), 0, FORMS.length - 1);
        int element = clamp((int) spell.param("element", 0.0D), 0, ELEMENTS.length - 1);
        Component shape = Component.translatable("original.frieren.form." + FORMS[form] + ".desc",
            fmt(spell.param("damage", 0.0D)), fmt(spell.param("range", 0.0D)), fmt(spell.param("radius", 0.0D)),
            (int) spell.param("count", 1.0D), fmt(spell.param("duration", 0.0D) / 20.0D), fmt(spell.param("heal", 0.0D)),
            fmt(spell.param("absorb", 0.0D)));
        String aspect = form == FORM_WARD ? ".ward" : form == FORM_MEND ? ".mend" : ".desc";
        Component out = Component.empty().append(shape).append(" ")
            .append(Component.translatable("original.frieren.element." + ELEMENTS[element] + aspect));
        if (spell.param("execute_max_health", 0.0D) >= 100000.0D) {
            out = out.copy().append(" ").append(Component.translatable("original.frieren.annihilation.desc"));
        } else if (spell.param("execute_max_health", 0.0D) > 0.0D) {
            out = out.copy().append(" ").append(Component.translatable("original.frieren.execute.desc",
                fmt(spell.param("execute_max_health", 0.0D))));
        }
        if (spell.isSteady()) {
            out = out.copy().append(" ").append(Component.translatable("original.frieren.steady.desc"));
        } else if (spell.isCharged() || spell.castTime() > 0) {
            out = out.copy().append(" ").append(Component.translatable("original.frieren.fragile.desc"));
        }
        return out.copy().append(" ").append(Component.translatable("original.frieren.signature"));
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
