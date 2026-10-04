"""
Generates assets/frieren/lang/en_us.json.

Every translation key used by the Java code or by generated data lives here. Argument placeholders match the
call sites exactly (checked by tools/validate_resources.py), because a placeholder index past the end of the
argument list makes Minecraft print the raw format string instead of the message.
All text is original; character names are used only as labels.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "frieren", "lang")
NS = "frieren"

L = {}


def put(key, value):
    if key in L:
        raise SystemExit(f"duplicate lang key {key}")
    L[key] = value


# ---------------------------------------------------------------------------------------------
# Registry names
# ---------------------------------------------------------------------------------------------
BLOCKS = {
    "mana_crystal_ore": "Mana Crystal Ore",
    "deepslate_mana_crystal_ore": "Deepslate Mana Crystal Ore",
    "mana_crystal_block": "Block of Mana Crystal",
    "spell_research_desk": "Spell Research Desk",
    "summoning_altar": "Summoning Altar",
    "cursed_gold_block": "Cursed Gold",
    "blue_moon_weed": "Blue Moon Weed",
    "mana_light": "Mana Light",
}
ITEMS = {
    "common_grimoire": "Common Grimoire",
    "uncommon_grimoire": "Uncommon Grimoire",
    "rare_grimoire": "Rare Grimoire",
    "ancient_grimoire": "Ancient Grimoire",
    "legendary_grimoire": "Legendary Grimoire",
    "demon_grimoire": "Demon Grimoire",
    "forbidden_grimoire": "Forbidden Grimoire",
    "sealed_demon_grimoire": "Sealed Demon Grimoire",
    "apprentice_staff": "Apprentice Staff",
    "mage_staff": "Mage Staff",
    "sages_staff": "Sage's Staff",
    "guillotine_staff": "Staff of the Guillotine",
    "mage_hat": "Mage Hat",
    "mage_coat": "Mage Coat",
    "mage_trousers": "Mage Trousers",
    "mage_boots": "Mage Boots",
    "mana_crystal": "Mana Crystal",
    "demon_horn": "Demon Horn",
    "demonic_mana_core": "Demonic Mana Core",
    "guillotine_blade": "Guillotine Blade",
    "mana_potion": "Mana Potion",
    "heiter_flask": "Heiter's Flask",
    "mirror_lotus_ring": "Mirror Lotus Ring",
    "concealment_charm": "Concealment Charm",
    "demon_hunter_insignia": "Demon Hunter Insignia",
    "exam_seal": "Exam Seal",
    "stille_cage": "Stille Cage",
    "sealed_exam_grimoire": "Sealed Exam Grimoire",
    "journey_journal": "Journey Journal",
    "sigil_of_the_guillotine": "Sigil of the Guillotine",
    "seal_of_corruption": "Seal of Corruption",
    "mirror_shard": "Mirror Shard",
    "demon_soldier_spawn_egg": "Demon Soldier Spawn Egg",
    "demon_mage_spawn_egg": "Demon Mage Spawn Egg",
    "aura_thrall_spawn_egg": "Headless Thrall Spawn Egg",
    "mimic_spawn_egg": "Mimic Spawn Egg",
    "stille_spawn_egg": "Stille Spawn Egg",
    "wandering_mage_spawn_egg": "Wandering Mage Spawn Egg",
    "examiner_spawn_egg": "Association Examiner Spawn Egg",
    "priest_spawn_egg": "Priest Spawn Egg",
    "frieren_spawn_egg": "Frieren Spawn Egg",
}
ENTITIES = {
    "demon_soldier": "Demon Soldier",
    "demon_mage": "Demon Mage",
    "aura_thrall": "Headless Thrall",
    "aura": "Aura the Guillotine",
    "qual": "Qual the Corruptor",
    "spiegel": "Spiegel",
    "mirror_replica": "Mirror Replica",
    "mimic": "Mimic",
    "stille": "Stille",
    "wandering_mage": "Wandering Mage",
    "examiner": "Association Examiner",
    "priest": "Priest",
    "frieren": "Frieren",
    "fern": "Fern",
    "stark": "Stark",
    "serie": "Serie",
    "mana_bolt": "Mana Bolt",
    "spell_area": "Spell Area",
    "mana_decoy": "Mana Decoy",
}
EFFECTS = {
    "enthralled": "Enthralled",
    "gilded": "Gilded",
    "warded": "Warded",
}


def registry_names():
    for k, v in BLOCKS.items():
        put(f"block.{NS}.{k}", v)
    for k, v in ITEMS.items():
        put(f"item.{NS}.{k}", v)
    for k, v in ENTITIES.items():
        put(f"entity.{NS}.{k}", v)
    for k, v in EFFECTS.items():
        put(f"effect.{NS}.{k}", v)
    put(f"itemGroup.{NS}.{NS}", "Frieren: Beyond Journey's End")
    put(f"item.{NS}.grimoire_of", "Grimoire of %s")
    put(f"entity.{NS}.mirror_replica.of", "Reflection of %s")


# ---------------------------------------------------------------------------------------------
# Spells
# ---------------------------------------------------------------------------------------------
SPELLS = {
    "zoltraak": ("Zoltraak", "Fires a piercing beam of killing magic. Once a demon's spell; now the most studied spell on the continent."),
    "zoltraak_barrage": ("Zoltraak Barrage", "Hold to loose a stream of Zoltraak bolts. Each pulse costs mana while the key is held."),
    "qual_zoltraak": ("Corruptor's Zoltraak", "The original demon version: a heavier beam that tears through barriers and pierces a target."),
    "vollzanbel": ("Vollzanbel", "Breathes a cone of hellfire that sets everything in front of you ablaze."),
    "judradjim": ("Judradjim", "Calls a lightning strike onto the point you are looking at, shocking all nearby."),
    "waldgose": ("Waldgose", "Summons a cutting whirlwind at a distance that drags in and shreds enemies."),
    "catastravia": ("Catastravia", "Fills the sky with shafts of light that rain down on an area for several seconds."),
    "reelseiden": ("Reelseiden", "A close-range slashing spell that cleaves through barriers in a wide arc."),
    "diagolze": ("Diagolze", "Forbidden. Curses a target into gold. Bosses resist; everyone else becomes a statue for a while."),
    "defensive_magic": ("Defensive Magic", "Toggle a personal barrier. Absorbs spell damage efficiently and blocks melee partially, draining mana while up."),
    "barrier_dome": ("Barrier Dome", "Raises a protective dome that shields allies inside from hostile magic."),
    "goddess_healing": ("Goddess Healing", "Priestly magic that heals the ally you look at, or yourself."),
    "curse_purification": ("Curse Purification", "Cleanses curses, enthrallment and harmful effects from you and nearby allies."),
    "flight_magic": ("Flight Magic", "Toggle flight. Drains mana every second; you will float down safely when it runs out."),
    "flower_field": ("Flower Field", "Makes a field of flowers bloom around you. Useless in combat. Absolutely essential."),
    "mimic_detection": ("Mimic Detection", "Reveals nearby mimics disguised as chests. Has a small chance of being wrong."),
    "clothes_cleaning": ("Clothes Cleaning", "Mends the durability of your worn equipment. A housekeeping spell with surprising value."),
    "rust_removal": ("Rust Removal", "Strips oxidation from copper blocks in front of you. Statues everywhere thank you."),
    "seeking_magic": ("Seeking Magic", "Points toward the nearest point of interest: your last death, or else a ruin worth exploring."),
    "light_orb": ("Light Orb", "Conjures a floating light where you look. It fades after a few minutes."),
    "gathering_magic": ("Gathering Magic", "Pulls nearby dropped items and experience toward you."),
    "azeryuze": ("Azeryuze", "Demon magic. Weighs your mana against a creature's; if yours is far greater, it obeys you for a while."),
    "lands_doppelganger": ("Land's Doppelganger", "Leaves a mana decoy behind that draws enemy attention while you slip away."),
}


def spells():
    for sid, (name, desc) in SPELLS.items():
        put(f"spell.{NS}.{sid}", name)
        put(f"spell.{NS}.{sid}.desc", desc)


# ---------------------------------------------------------------------------------------------
# Enums: ranks, rarities, schools, exam stages, quests
# ---------------------------------------------------------------------------------------------
RANKS = {
    "unranked": "Unranked",
    "fifth_class": "Fifth-Class Mage",
    "fourth_class": "Fourth-Class Mage",
    "third_class": "Third-Class Mage",
    "second_class": "Second-Class Mage",
    "first_class": "First-Class Mage",
}
RARITIES = {
    "common": "Common", "uncommon": "Uncommon", "rare": "Rare", "ancient": "Ancient",
    "legendary": "Legendary", "demon": "Demon Magic", "forbidden": "Forbidden",
}
SCHOOLS = {"offensive": "Offensive", "defensive": "Defensive", "utility": "Utility", "special": "Special"}
STAGES = {
    "practical": ("Practical", "Defeat hostile creatures using only magic."),
    "survival": ("Survival", "Stay alive until time runs out and defeat demons along the way."),
    "retrieval": ("Retrieval", "Find the sealed grimoire hidden in a Forgotten Library and bring it to an examiner."),
    "labyrinth": ("Labyrinth", "Reach the heart of a dungeon and face the reflection waiting there."),
    "stille": ("Catch the Stille", "Capture a Stille in a Stille Cage and hand it over. Brute force will not work."),
    "interview": ("Interview", "Speak with Serie. She will look straight through your concealment."),
}
QUESTS = {
    "journey_begins": ("The Journey Begins", "Learn your first spell from a grimoire.", "Apprentice Staff"),
    "a_field_of_flowers": ("A Field of Flowers", "Cast Flower Field 10 times.", "4 Blue Moon Weed"),
    "the_heros_statue": ("The Hero's Statue", "Remove rust from 24 copper blocks.", "Mirror Lotus Ring"),
    "mimic_hunter": ("Mimic Hunter", "Research mimics: find 3 of them, the hard way if necessary.", "An unidentified Rare grimoire"),
    "spell_collector": ("Spell Collector", "Know 10 different spells.", "+25 permanent mana growth"),
    "demon_slayer": ("Demon Slayer", "Slay 25 demons.", "Demon Hunter Insignia"),
    "aura_kill_yourself": ("Aura, Kill Yourself", "Defeat Aura the Guillotine.", "+60 mana growth and an unidentified Legendary grimoire"),
}


def enums():
    for k, v in RANKS.items():
        put(f"rank.{NS}.{k}", v)
    for k, v in RARITIES.items():
        put(f"rarity.{NS}.{k}", v)
    for k, v in SCHOOLS.items():
        put(f"school.{NS}.{k}", v)
    for k, (title, desc) in STAGES.items():
        put(f"exam.stage.{NS}.{k}", title)
        put(f"exam.stage.{NS}.{k}.desc", desc)
    for k, (title, desc, reward) in QUESTS.items():
        put(f"quest.{NS}.{k}", title)
        put(f"quest.{NS}.{k}.desc", desc)
        put(f"quest.{NS}.{k}.reward", "Reward: " + reward)


# ---------------------------------------------------------------------------------------------
# Tooltips
# ---------------------------------------------------------------------------------------------
def tooltips():
    t = lambda k, v: put(f"tooltip.{NS}.{k}", v)
    t("mana_crystal", "Crystallised mana. Fuel for research and transcription.")
    t("demon_horn", "Still warm. Demons are never as harmless as they sound.")
    t("demonic_mana_core", "The condensed mana of a slain demon.")
    t("guillotine_blade", "Aura's weapon. The scales are gone, but the edge remains.")
    t("sealed_exam_grimoire", "The exam objective. Return it to an examiner unopened.")
    t("mage_attire", "Mage's Attire: each piece adds max mana and regeneration.")
    t("heiter_flask", "Never empties itself, and never quite sobers you up.")
    t("staff_none", "No focus.")
    for tier in ("apprentice", "mage", "sage", "guillotine"):
        t(f"staff_{tier}", "A spellcasting focus. Hold it in either hand while casting.")
    t("staff_stats", "-%s%% mana cost, -%s%% cooldown, +%s%% spell power")
    t("sigil_aura", "Summons Aura the Guillotine.")
    t("sigil_qual", "Summons Qual the Corruptor.")
    t("sigil_spiegel", "Summons Spiegel, the mirror of the labyrinth.")
    t("sigil_hint", "Use on a Summoning Altar.")
    t("mirror_lotus_ring", "A ring said to mean eternal love. Polished, not appraised.")
    t("mirror_lotus_ring.effect", "+25% mana regeneration")
    t("concealment_charm", "Keeps a mage's mana quiet.")
    t("concealment_charm.effect", "Concealment hides 15% more mana and trains twice as fast")
    t("demon_hunter_insignia", "Proof of a demon-slayer's career.")
    t("demon_hunter_insignia.effect", "+20% spell damage against demons")
    t("artifact_hint", "Works while in your hotbar or offhand.")
    t("exam_seal", "Use to see your current exam objective.")
    t("grimoire_sealed", "Sealed. Analyse it at a Spell Research Desk.")
    t("grimoire_unidentified", "Unidentified. Read it to discover its spell.")
    t("journey_journal", "Use to open your journey: rank, mana and quests.")
    t("requires_rank", "Requires: %s")
    t("restores_mana", "Restores %s mana")
    t("spell_cost", "%s mana, %ss cooldown")
    t("stille_cage", "Empty. Use on an exhausted Stille.")
    t("stille_cage_full", "Holds a captured Stille.")


# ---------------------------------------------------------------------------------------------
# Messages, HUD, screens
# ---------------------------------------------------------------------------------------------
def messages():
    m = lambda k, v: put(f"message.{NS}.{k}", v)
    m("altar_busy", "Something already answers this altar.")
    m("altar_hint", "The altar waits for a sigil.")
    m("barrier_broken", "Your barrier shattered!")
    m("barrier_off", "Defensive magic lowered")
    m("barrier_on", "Defensive magic raised")
    m("cast_interrupted", "Your spell was interrupted!")
    m("casting", "Casting %s...")
    m("conceal_off", "Mana released (%s%% concealed)")
    m("conceal_on", "Mana concealed (%s%% hidden)")
    m("cooldown", "%s is on cooldown (%ss)")
    m("deceived", "%s was never going to let you live.")
    m("desk_analysed", "Analysis complete: %s")
    m("desk_copied", "Transcribed: %s")
    m("desk_copy_cost", "Transcription needs a book, %s mana crystals and a spell you know.")
    m("desk_cost", "Analysis needs %s mana crystals and %s mana.")
    m("desk_help", "Hold a sealed demon grimoire to analyse it, or a known grimoire and a book to transcribe it.")
    m("desk_rank", "Only a %s or higher can analyse demon magic.")
    m("desk_unknown_spell", "You can only transcribe a spell you already know.")
    m("empty_slot", "No spell in this slot. Open the spellbook to assign one.")
    m("enthralled", "Your body moves on its own...")
    m("flight_disabled", "Flight is disabled on this server.")
    m("flight_exhausted", "Out of mana. You drift down.")
    m("flight_off", "Flight ended")
    m("flight_on", "Flight started")
    m("grimoire_empty", "The pages are blank.")
    m("grimoire_known", "You already know %s.")
    m("grimoire_learned", "You learned %s!")
    m("grimoire_rank", "%s is beyond you. Requires %s.")
    m("grimoire_sealed", "It is sealed. Analyse it at a Spell Research Desk.")
    m("mana_bonus", "Your mana capacity grew by %s (now %s).")
    m("mana_capped", "Your mana will not grow further at this rank.")
    m("mana_grew", "Your mana capacity grew to %s.")
    m("mend_nothing", "Your equipment is already pristine.")
    m("mimic_bite", "It was a mimic!")
    m("mimics_false_negative", "No mimics nearby... probably.")
    m("mimics_found", "Revealed %s mimic(s)!")
    m("mimics_none", "No mimics nearby.")
    m("missing_definition", "Unknown spell %s. The data pack that added it is missing.")
    m("no_health", "You are too hurt to pay this spell's price.")
    m("no_mana", "Not enough mana (needs %s)")
    m("obedience_fail", "%s resists your mana.")
    m("obedience_success", "%s obeys you.")
    m("quest_complete", "Quest complete: %s")
    m("seal_idle", "No exam in progress. Talk to an Association Examiner.")
    m("seal_objective", "Objective offset: X %s, Z %s (%s blocks away)")
    m("seal_status", "%s exam - %s - %s left")
    m("seek_death", "Your last death lies %s, %s blocks away.")
    m("seek_nothing", "Your seeking magic finds nothing of interest.")
    m("seek_ruin", "A ruin lies %s, %s blocks away.")
    m("sense_count", "You sense %s mana signature(s).")
    m("sense_through", "You sense %s signature(s), and one of them is hiding its true mana.")
    m("stille_caught", "You caught the Stille!")
    m("stille_not_yours", "That cage belongs to someone else's exam.")
    m("stille_too_fast", "It is still too lively. Tire it out first.")
    m("summoned.aura", "Aura the Guillotine answers the summons.")
    m("summoned.qual", "Qual the Corruptor emerges from his seal.")
    m("summoned.spiegel", "The mirror shatters, and Spiegel steps out.")
    m("trade_cannot_afford", "You cannot afford that.")
    m("trade_done", "Bought %s")
    m("unknown_spell", "You do not know that spell.")

    for d, v in {"north": "north", "northeast": "northeast", "east": "east", "southeast": "southeast",
                 "south": "south", "southwest": "southwest", "west": "west", "northwest": "northwest"}.items():
        put(f"direction.{NS}.{d}", v)

    h = lambda k, v: put(f"hud.{NS}.{k}", v)
    h("barrier", "Barrier")
    h("casting", "Casting")
    h("channeling", "Channeling")
    h("concealed", "Concealed %s%%")
    h("flight", "Flight")


def bosses_and_npcs():
    b = lambda k, v: put(f"boss.{NS}.{k}", v)
    b("aura.enthrall", "Aura's scales tipped against %s. Their body is no longer their own.")
    b("aura.obey", "The scales tip toward %s. Aura is commanded to kill herself!")
    b("aura.scales", "Aura weighs her mana against %s's...")
    b("aura.weighing", "Aura raises the Scales of Obedience toward %s. Release your concealment now!")
    b("spiegel.exposed", "The last replica breaks. Spiegel is exposed!")
    b("spiegel.immune", "Spiegel cannot be harmed while its reflections remain.")
    b("spiegel.reflect", "Spiegel copies a reflection of you.")

    for phase, v in {"command": "Aura commands her headless army!", "scales": "Aura takes up the Scales of Obedience!",
                     "guillotine": "Aura draws the Guillotine blade!"}.items():
        put(f"aura.{NS}.phase.{phase}", v)

    pleas = ["Please, I don't want to die...", "Mother... I only wanted to see my mother.",
             "Let's talk. Surely we can understand each other.", "I surrender. I'll never harm a human again."]
    for i, v in enumerate(pleas):
        put(f"demon.{NS}.plea.{i}", v)

    npc = {
        "frieren": ["I'm collecting spells. Got any grimoires?", "Ten years is not very long, you know.",
                    "Flowers are nice. That's the whole reason.", "I'll check that chest. It is definitely not a mimic.",
                    "Humans grow so fast."],
        "fern": ["Please don't waste mana.", "Basic attack magic is enough if you're fast.", "We're behind schedule."],
        "stark": ["I'm not scared. My hands are just cold.", "Leave the front line to me.", "Is there food yet?"],
        "wandering_mage": ["I trade grimoires for mana crystals.", "The roads are full of demons lately.",
                           "Someone always buys Flower Field. Always."],
        "priest": ["May the Goddess watch over your journey.", "Rest a moment. I can mend that.",
                   "Holy water? No, this is just water."],
        "examiner": ["The Association tests every mage the same way.", "Your rank is earned, not bought.",
                     "Speak to me when you are ready for your exam."],
        "serie": ["Your mana is as plain as your face.", "Peace is boring.", "Magic is only what you imagine it to be."],
    }
    for npc_id, lines in npc.items():
        for i, v in enumerate(lines):
            put(f"npc.{NS}.{npc_id}.line.{i}", v)
    put(f"npc.{NS}.priest.heal", "The Goddess's light washes over you.")
    put(f"npc.{NS}.serie.interview", "Serie studies you for a long time. Then she makes up her mind.")


def exams():
    e = lambda k, v: put(f"exam.{NS}.{k}", v)
    e("already_active", "You are already taking an exam.")
    e("broadcast", "%s has become a %s!")
    e("fail.abandoned", "you abandoned it")
    e("fail.death", "you died")
    e("fail.time", "time ran out")
    e("failed", "Exam failed: %s.")
    e("max_rank", "You hold the highest rank already.")
    e("not_eligible", "You are not yet eligible for the %s exam:")
    e("nothing_to_submit", "You have nothing the examiner is waiting for.")
    e("privilege", "Serie's privilege: choose one Legendary spell.")
    e("privilege_claimed", "Serie grants you %s.")
    e("privilege_none", "You have no privilege to claim.")
    e("progress", "Exam progress: %s / %s")
    e("progress_survival", "Exam progress: %s / %s seconds, %s / %s demons")
    e("req.defensive", "Know a defensive spell")
    e("req.mana", "Max mana %s (you have %s)")
    e("req.mastery", "%s spells at mastery %s (you have %s)")
    e("req.rank", "Hold the rank %s")
    e("req.spells", "Know %s spells (you know %s)")
    e("serie_fail", "\"No.\" Serie turns away. Train and come back.")
    e("serie_pass", "\"Fine. You pass.\" Serie seems almost amused.")
    e("stage", "Stage %s of %s: %s")
    e("stage_passed", "Stage complete!")
    e("started", "Your %s exam has begun. Use the Exam Seal to check your objective.")


def screens():
    s = lambda k, v: put(f"screen.{NS}.{k}", v)
    s("examiner", "Continental Magic Association")
    s("examiner.abandon", "Abandon Exam")
    s("examiner.begin", "Begin Exam")
    s("examiner.privilege", "Claim %s")
    s("examiner.submit", "Submit Item")
    s("journey", "Journey Journal")
    s("journey.conceal", "Concealment mastery: %s (level %s)")
    s("journey.detect", "Detection mastery: %s (power %s)")
    s("journey.eligible", "You are eligible for the next exam.")
    s("journey.exam_active", "%s exam - %s - %s left")
    s("journey.growth", "Mana growth: %s / %s (+%s bonus)")
    s("journey.mana", "Mana: %s / %s")
    s("journey.next_exam", "Next exam: %s")
    s("journey.quests", "Quests")
    s("journey.rank", "Rank: %s")
    s("journey.spells", "Spells known: %s")
    s("journey.training", "Training: %s / %s")
    s("spellbook", "Spellbook")
    s("spellbook.cast_time", "Cast time: %ss")
    s("spellbook.cost", "Cost: %s mana, cooldown %ss")
    s("spellbook.empty", "(empty)")
    s("spellbook.hint", "Click a spell, then a slot to assign it. Right-click a slot to clear it.")
    s("spellbook.mastery", "Mastery %s (%s casts)")
    s("spellbook.none", "You know no spells. Read a grimoire.")
    s("trades", "Trades")
    s("trades.buy", "Buy")
    s("trades.frieren", "Frieren's Trades")
    s("trades.priest", "Priest's Offerings")
    s("trades.wandering_mage", "Wandering Mage's Wares")


def commands():
    c = lambda k, v: put(f"commands.{NS}.{k}", v)
    c("cooldowns", "Cleared spell cooldowns for %s")
    c("exam.passed", "%s passed the exam and is now a %s")
    c("mana.filled", "Filled the mana of %s")
    c("mana.get", "%s: mana %s / %s, growth %s (+%s bonus)")
    c("mana.growth", "Gave %s %s mana growth (max mana now %s)")
    c("mana.set", "Set the mana of %s to %s")
    c("no_data", "That player has no magic data.")
    c("quest.already", "Quest %s is already complete.")
    c("quest.reset", "Reset quest %s for %s")
    c("quest.unknown", "Unknown quest %s")
    c("rank.get", "%s is a %s")
    c("rank.set", "%s is now a %s")
    c("rank.unknown", "Unknown rank %s")
    c("reset", "Reset all magic data of %s")
    c("spell.already", "%s already knows %s")
    c("spell.given", "Taught %s to %s")
    c("spell.learnall", "Taught %s spells to %s")
    c("spell.list", "%s knows %s spells:")
    c("spell.mastery", "Set mastery of %s to %s for %s")
    c("spell.not_known", "%s does not know %s")
    c("spell.removed", "Removed %s from %s")
    c("spell.unknown", "Unknown spell %s")


def keys():
    put(f"key.category.{NS}.magic", "Frieren: Magic")
    for k, v in {"cast": "Cast Spell (hold to channel)", "cycle_spell": "Next Spell Slot", "spellbook": "Open Spellbook",
                 "conceal": "Toggle Mana Concealment", "detect": "Sense Mana", "journal": "Open Journey Journal"}.items():
        put(f"key.{NS}.{k}", v)


def death_messages():
    # msgId "frieren.<name>": vanilla appends ".player" (no direct/causing entity, has kill credit) and ".item"
    # (named weapon). Spell damage always carries the caster, so the base key receives two arguments.
    deaths = {
        "spell": ("%1$s was blasted apart by %2$s's magic", "%1$s was blasted apart by magic while fighting %2$s",
                  "%1$s was blasted apart by %2$s using %3$s"),
        "zoltraak": ("%1$s was pierced by %2$s's Zoltraak", "%1$s was pierced by Zoltraak while fighting %2$s",
                     "%1$s was pierced by %2$s's Zoltraak cast through %3$s"),
        "demon_magic": ("%1$s fell to %2$s's demon magic", "%1$s fell to demon magic while fighting %2$s",
                        "%1$s fell to %2$s's demon magic wielding %3$s"),
        "curse": ("%1$s was cursed by %2$s", "%1$s succumbed to a curse while fighting %2$s",
                  "%1$s was cursed by %2$s using %3$s"),
        "guillotine": ("%1$s was beheaded by %2$s", "%1$s lost their head while fighting %2$s",
                       "%1$s was beheaded by %2$s with %3$s"),
    }
    for name, (base, player, item) in deaths.items():
        put(f"death.attack.{NS}.{name}", base)
        put(f"death.attack.{NS}.{name}.player", player)
        put(f"death.attack.{NS}.{name}.item", item)


ADVANCEMENTS = {
    "root": ("Beyond Journey's End", "Receive a Journey Journal and begin your life as a mage"),
    "first_spell": ("Basic Magic", "Learn your first spell from a grimoire"),
    "rank_fifth_class": ("Fifth-Class Mage", "Pass the Fifth-Class exam"),
    "rank_fourth_class": ("Fourth-Class Mage", "Pass the Fourth-Class exam"),
    "rank_third_class": ("Third-Class Mage", "Pass the Third-Class exam"),
    "rank_second_class": ("Second-Class Mage", "Pass the Second-Class exam"),
    "rank_first_class": ("First-Class Mage", "Pass Serie's interview and become a First-Class Mage"),
    "learned_demon": ("Know Your Enemy", "Learn a spell of demon magic"),
    "learned_forbidden": ("Forbidden Knowledge", "Learn a forbidden spell"),
    "slay_demon": ("They Speak, But Do Not Understand", "Slay a demon"),
    "aura_obeyed": ("Aura, Kill Yourself", "Win the weighing of the Scales of Obedience"),
    "quest_aura_kill_yourself": ("The Guillotine Falls", "Defeat Aura the Guillotine"),
    "quest_mimic_hunter": ("It Was Definitely a Mimic", "Complete the Mimic Hunter quest"),
    "quest_the_heros_statue": ("Polished Memories", "Restore the Hero's statue"),
    "quest_a_field_of_flowers": ("Her Favourite Spell", "Bloom ten flower fields"),
}


def advancements():
    for k, (title, desc) in ADVANCEMENTS.items():
        put(f"advancements.{NS}.{k}.title", title)
        put(f"advancements.{NS}.{k}.description", desc)


SUBTITLES = {
    "spell.cast": "Spell cast",
    "spell.fail": "Spell fizzles",
    "spell.zoltraak": "Zoltraak fires",
    "spell.barrier_hit": "Barrier absorbs",
    "spell.barrier_break": "Barrier shatters",
    "spell.heal": "Healing magic",
    "spell.flower_bloom": "Flowers bloom",
    "mana.conceal": "Mana concealed",
    "mana.release": "Mana released",
    "mana.detect": "Mana sensed",
    "grimoire.learn": "Spell learned",
    "rank.up": "Rank up",
    "demon.speak": "Demon speaks",
    "aura.scales": "Scales weigh",
    "mimic.chomp": "Mimic bites",
    "magic.teleport": "Magic blink",
    "spell.hellfire": "Hellfire roars",
    "spell.wind": "Whirlwind howls",
    "spell.light_rain": "Light rains down",
    "spell.slash": "Magic slash",
    "spell.curse": "Curse takes hold",
    "spell.barrier_up": "Barrier raised",
    "spell.flight": "Flight magic",
    "spell.utility": "Utility magic",
    "demon.bolt": "Demon magic fires",
    "boss.roar": "Boss roars",
}


def subtitles():
    for k, v in SUBTITLES.items():
        put(f"subtitles.{NS}.{k}", v)


def main():
    registry_names()
    spells()
    enums()
    tooltips()
    messages()
    bosses_and_npcs()
    exams()
    screens()
    commands()
    keys()
    death_messages()
    advancements()
    subtitles()
    os.makedirs(ROOT, exist_ok=True)
    with open(os.path.join(ROOT, "en_us.json"), "w", encoding="utf-8") as f:
        json.dump(dict(sorted(L.items())), f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"wrote {len(L)} lang entries")


if __name__ == "__main__":
    main()
