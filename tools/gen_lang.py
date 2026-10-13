"""
Generates assets/frieren/lang/en_us.json.

Every translation key used by the Java code or by generated data lives here. Argument placeholders match the
call sites exactly (checked by tools/validate_resources.py), because a placeholder index past the end of the
argument list makes Minecraft print the raw format string instead of the message.
All text is original; character names are used only as labels.
"""
import json
import os
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_building  # noqa: E402

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
    "mana_bridge": "Bridge of Light",
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
    "warriors_axe": "Warrior's Axe",
    "heros_sword": "Replica of the Hero's Sword",
    "demon_kings_blade": "Blade of the Demon King",
    "scale_of_obedience": "Scale of Obedience",
    "mage_hat": "Mage Hat",
    "mage_coat": "Mage Coat",
    "mage_trousers": "Mage Trousers",
    "mage_boots": "Mage Boots",
    "mana_crystal": "Mana Crystal",
    "demon_horn": "Demon Horn",
    "demonic_mana_core": "Demonic Mana Core",
    "guillotine_blade": "Guillotine Blade",
    "mana_potion": "Mana Potion",
    "sweet_grapes": "Sweet Grapes",
    "heiter_flask": "Heiter's Flask",
    "mirror_lotus_ring": "Mirror Lotus Ring",
    "concealment_charm": "Concealment Charm",
    "demon_hunter_insignia": "Demon Hunter Insignia",
    "exam_seal": "Exam Seal",
    "stille_cage": "Stille Cage",
    "sealed_exam_grimoire": "Sealed Exam Grimoire",
    "journey_journal": "Journey Journal",
    "sigil_of_the_guillotine": "Sigil of the Guillotine",
    "seal_of_the_golden_land": "Seal of the Golden Land",
    "seal_of_the_demon_king": "Seal of the Demon King",
    "seal_of_corruption": "Seal of Corruption",
    "mirror_shard": "Mirror Shard",
    "demon_soldier_spawn_egg": "Demon Soldier Spawn Egg",
    "demon_mage_spawn_egg": "Demon Mage Spawn Egg",
    "aura_thrall_spawn_egg": "Headless Thrall Spawn Egg",
    "mimic_spawn_egg": "Mimic Spawn Egg",
    "stille_spawn_egg": "Stille Spawn Egg",
    "linie_spawn_egg": "Linie Spawn Egg",
    "draht_spawn_egg": "Draht Spawn Egg",
    "lugner_spawn_egg": "Lügner Spawn Egg",
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
    "mirror_frieren": "Frieren's Reflection",
    "demon_king": "The Demon King",
    "macht": "Macht of the Golden Land",
    "linie": "Linie the Imitator",
    "draht": "Draht the Wire",
    "lugner": "Lügner the Liar",
    "exam_construct": "Exam Construct",
    "hero_statue": "Statue of Himmel the Hero",
    "himmel_echo": "Himmel",
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
    "magic_circle": "Magic Circle",
    "spell_beam": "Spell Beam",
    "barrier_shell": "Barrier Shell",
}
EFFECTS = {
    "enthralled": "Enthralled",
    "gilded": "Gilded",
    "warded": "Warded",
    "bound": "Bound",
    "reflecting": "Reflecting",
}


def registry_names():
    for k, v in {**BLOCKS, **gen_building.NAMES}.items():
        put(f"block.{NS}.{k}", v)
    for k, v in ITEMS.items():
        put(f"item.{NS}.{k}", v)
    for k, v in ENTITIES.items():
        put(f"entity.{NS}.{k}", v)
    for k, v in EFFECTS.items():
        put(f"effect.{NS}.{k}", v)
    put(f"itemGroup.{NS}.frieren_magic", "Frieren: Magic & Grimoires")
    put(f"itemGroup.{NS}.frieren_gear", "Frieren: Staffs, Weapons & Attire")
    put(f"itemGroup.{NS}.frieren_building", "Frieren: Building")
    put(f"itemGroup.{NS}.frieren_characters", "Frieren: Characters & Bosses")
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
    "seeking_magic": ("Seeking Magic", "Points toward your last death, or else the nearest unopened treasure chest (or something pretending to be one)."),
    "frost_lance": ("Frost Lance", "Lawine-style ice magic: a fan of ice shards that chill and slow what they strike."),
    "earth_spikes": ("Earth Spikes", "The ground erupts in a line before you, battering and launching everything on it."),
    "water_whip": ("Water Whip", "A lash of water sweeps the area in front of you, knocking foes back and dousing flames."),
    "zoltraak_full_power": ("Zoltraak: Full Power", "Ordinary offensive magic, poured into without restraint. A long wind-up, then a beam that pierces everything in its path."),
    "mana_release": ("Mana Release", "Drop all restraint: half your remaining mana explodes outward. The more you have, the bigger the blast."),
    "blood_blades": ("Lügner's Blood Blades", "Demon blood magic. A fan of crimson blades; each one feeds you part of the wound it opens. Costs health."),
    "wire_snare": ("Draht's Wire Snare", "Demon thread magic. Invisible wires lash out from a point and bind every enemy around it."),
    "sorganeil": ("Sorganeil", "Whatever you hold in your gaze cannot move. Keep casting and keep looking; look away and it is free."),
    "reflection_barrier": ("Reflection Barrier", "For a few seconds, projectiles and spells that strike you are turned back at their source."),
    "goddess_sanctuary": ("Goddess's Sanctuary", "A circle of holy light that heals allies standing in it and puts out flames."),
    "bottled_golem": ("Serie's Bottled Golem", "The guardian the Association hands to First-Class examinees. A stone golem fights at your side for a while."),
    "wind_step": ("Wind Step", "A gust carries you forward and softens your landing."),
    "harvest_magic": ("The Spell to Harvest Crops", "Every ripe crop around you is harvested and replanted on the spot."),
    "cooking_magic": ("The Spell to Cook a Meal", "Cooks the raw food you are carrying. Heiter would have approved."),
    "grape_sweetening": ("The Spell to Turn Sour Grapes Sweet", "Utterly useless and absolutely delightful: sweet berries become Sweet Grapes."),
    "clear_skies": ("The Spell to Clear the Skies", "Ends rain and thunder for a while. Perfect weather for a journey."),
    "light_orb": ("Light Orb", "Conjures a floating light where you look. It fades after a few minutes."),
    "gathering_magic": ("Gathering Magic", "Pulls nearby dropped items and experience toward you."),
    "azeryuze": ("Azeryuze", "Demon magic. Weighs your mana against a creature's; if yours is far greater, it obeys you for a while."),
    "lands_doppelganger": ("Land's Doppelganger", "Leaves a mana decoy behind that draws enemy attention while you slip away."),
    "tree_felling": ("The Spell to Fell a Tree", "Look at a living tree and it comes down in one stroke, every log at once. Log cabins are spared."),
    "field_blessing": ("The Spell to Bless the Fields", "Crops, saplings and berry bushes around you grow as if a season had passed."),
    "ore_finding": ("The Spell to Find Glittering Stones", "Feels the ores hidden around you and tells you how many there are and where the nearest of each lies."),
    "tunnel_carving": ("The Spell to Carve a Tunnel", "Opens a 3x3 passage the way you face, up and down included. Stone drops as if mined."),
    "return_home": ("The Spell to Return Home", "A long incantation that carries you to your bed, or to the world's spawn if you have none."),
    "light_bridge": ("The Spell to Lay a Bridge of Light", "A level walkway of solid light runs out from under your feet. It fades after a minute."),
    "night_sight": ("The Spell to See in the Dark", "You and your companions see in the dark for five minutes."),
    "fish_breath": ("The Spell to Breathe Like a Fish", "You and your companions can breathe underwater for three minutes."),
    "swift_hands": ("The Spell to Work Swiftly", "Your hands move twice as fast for a while. Mining has never been so pleasant."),
    "feather_fall": ("The Spell to Fall Like a Feather", "You and your companions drift down gently for half a minute."),
    "flame_ward": ("The Spell to Ward Off Flames", "Fire and lava cannot burn you or your companions for a minute and a half."),
    "smelting_magic": ("The Spell to Smelt Ore", "Fires the raw metal, sand, stone, clay and logs you carry as a furnace would."),
    "pocket_space": ("The Spell to Open a Pocket of Space", "Reach into your ender chest from anywhere."),
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
    "practical": ("Practical", "Six mana constructs drift around the exam circle. Break all of them with spells; nothing else counts."),
    "survival": ("Survival", "Hold the exam circle against three waves of Aura's soldiers and demons."),
    "retrieval": ("Grimoire Hunt", "Five chests appear. One is genuine, the rest are mimics. Open the real one. (Mimic Detection helps... mostly.)"),
    "labyrinth": ("Spiegel's Mirror", "Spiegel hides behind replicas of you. Break every reflection, then strike while it is exposed."),
    "stille": ("Catch the Stille", "Tire out the Stille and catch it with your Stille Cage. Brute force will not work."),
    "replica": ("Mirror of Frieren", "Defeat the mirror replica of Frieren. Her defence drops for a moment whenever she attacks."),
    "interview": ("Interview", "Serie is waiting. Detect her true mana in the instant her concealment flickers."),
}
QUESTS = {
    "journey_begins": ("The Journey Begins", "Learn your first spell from a grimoire.", "Apprentice Staff"),
    "a_field_of_flowers": ("A Field of Flowers", "Cast Flower Field 10 times.", "4 Blue Moon Weed"),
    "the_heros_statue": ("The Hero's Statue", "Remove rust from 24 copper blocks.", "Mirror Lotus Ring"),
    "mimic_hunter": ("Mimic Hunter", "Research mimics: find 3 of them, the hard way if necessary.", "An unidentified Rare grimoire"),
    "spell_collector": ("Spell Collector", "Know 10 different spells.", "+25 permanent mana growth"),
    "demon_slayer": ("Demon Slayer", "Slay 25 demons.", "Demon Hunter Insignia"),
    "aura_kill_yourself": ("Aura, Kill Yourself", "Defeat Aura the Guillotine.", "+60 mana growth and an unidentified Legendary grimoire"),
    "auras_generals": ("Aura's Generals", "Defeat Linie, Draht and Lügner.", "+30 mana growth and an unidentified Ancient grimoire"),
    "the_golden_land": ("The Golden Land", "Defeat Macht of the Golden Land.", "+60 mana growth and an unidentified Legendary grimoire"),
    "journeys_end": ("Journey's End", "Defeat the Demon King, as Himmel's party once did.", "+120 mana growth and two unidentified Legendary grimoires"),
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
    t("warriors_axe", "Eisen's kind of axe. Stark swears he's scared every time he swings it.")
    t("heros_sword", "Himmel never pulled the real one from its stone. He saved the world with this anyway.")
    t("demon_kings_blade", "Forged before humanity had a word for magic.")
    t("scale_of_obedience", "It weighs mana and never lies, which is more than its owner could say.")
    t("sealed_exam_grimoire", "The exam objective. Return it to an examiner unopened.")
    t("mage_attire", "Mage's Attire: each piece adds max mana and regeneration.")
    t("heiter_flask", "Never empties itself, and never quite sobers you up.")
    t("sweet_grapes", "Formerly sour. Frieren insists this is the most important spell she knows.")
    t("staff_none", "No focus.")
    for tier in ("apprentice", "mage", "sage", "guillotine"):
        t(f"staff_{tier}", "A spellcasting focus. Hold it in either hand while casting.")
    t("staff_stats", "-%s%% mana cost, -%s%% cooldown, +%s%% spell power")
    t("sigil_aura", "Summons Aura the Guillotine.")
    t("sigil_qual", "Summons Qual the Corruptor.")
    t("sigil_spiegel", "Summons Spiegel, the mirror of the labyrinth.")
    t("sigil_macht", "Summons Macht of the Golden Land, Sage of Destruction.")
    t("sigil_demon_king", "Summons the Demon King. Himmel's party needed all four of them.")
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
    m("true_chest_hit", "It's a real chest. Hitting it won't help.")
    m("no_target", "No target in sight.")
    m("bind_broken", "You looked away. Sorganeil releases its hold.")
    m("release_too_little", "Not enough mana to release.")
    m("harvest_nothing", "Nothing here is ripe yet.")
    m("harvest_done", "Harvested and replanted %s crop(s).")
    m("cook_nothing", "You have nothing raw to cook.")
    m("cook_done", "Cooked %s item(s).")
    m("sweeten_nothing", "You have no sour grapes (sweet berries) to sweeten.")
    m("sweeten_done", "%s berries are now Sweet Grapes.")
    m("weather_no_sky", "There is no sky here to clear.")
    m("weather_cleared", "The clouds part.")
    m("fell_no_tree", "Look at the trunk of a tree.")
    m("fell_not_living", "That is not a living tree. The spell will not fell a house.")
    m("fell_done", "Felled %s log(s).")
    m("growth_nothing", "Nothing around you can grow any further.")
    m("growth_done", "%s plant(s) grew.")
    m("ore_sense_nothing", "No ore within %s blocks.")
    m("ore_sense_header", "Ores within %s blocks:")
    m("ore_sense_line", "  %s: %s, nearest %s blocks away, %s")
    for key, name in (("coal", "Coal"), ("copper", "Copper"), ("iron", "Iron"), ("gold", "Gold"), ("redstone", "Redstone"),
                      ("lapis", "Lapis"), ("diamond", "Diamond"), ("emerald", "Emerald"), ("mana_crystal", "Mana Crystal")):
        m(f"ore.{key}", name)
    m("dir.below", "%s below, %s")
    m("dir.above", "%s above, %s")
    m("dir.level", "level, %2$s")
    for key, name in (("north", "north"), ("south", "south"), ("east", "east"), ("west", "west"),
                      ("north_east", "north-east"), ("north_west", "north-west"), ("south_east", "south-east"),
                      ("south_west", "south-west"), ("here", "right here")):
        m(f"dir.{key}", name)
    m("tunnel_nothing", "Nothing here the spell can carve.")
    m("tunnel_done", "Carved %s block(s).")
    m("recall_done", "You are home.")
    m("bridge_nothing", "There is no gap here to bridge.")
    m("smelt_nothing", "You carry nothing a furnace could fire.")
    m("smelt_done", "Smelted %s item(s).")
    m("statue_inscription", "\"Himmel the Hero, who defeated the Demon King and brought peace to the land.\"")
    m("statue_stage.0", "Green with verdigris. Nobody has polished it in a long time.")
    m("statue_stage.1", "Weathered, but someone remembers to visit.")
    m("statue_stage.2", "The copper shows through the patina.")
    m("statue_stage.3", "It gleams. Himmel would have loved how handsome it looks.")
    m("statue_polished", "Himmel's statue gleams like the day it was cast. Somewhere, he is probably smiling.")
    m("statue_already_clean", "The statue is already spotless.")
    m("himmel_echo.0", "Himmel: \"Thanks for keeping me handsome. I wanted to be remembered... mostly by one particular elf.\"")
    m("himmel_echo.1", "Himmel: \"Even a long journey goes by quickly with good friends. Don't walk it alone.\"")
    m("himmel_echo.2", "Himmel: \"You came all this way just to polish a statue? I'd have done the same.\"")
    m("himmel_echo.3", "Himmel: \"Don't forget to smile once in a while. It suits you.\"")
    m("construct_magic_only", "Only magic counts in the practical exam.")
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
    m("seek_treasure", "Something unopened lies %s, %s blocks away.")
    m("sense_count", "You sense %s mana signature(s).")
    m("sense_through", "You sense %s signature(s), and one of them is hiding its true mana.")
    m("stille_caught", "You caught the Stille!")
    m("stille_not_yours", "That cage belongs to someone else's exam.")
    m("stille_too_fast", "It is still too lively. Tire it out first.")
    m("summoned.aura", "Aura the Guillotine answers the summons.")
    m("summoned.qual", "Qual the Corruptor emerges from his seal.")
    m("summoned.spiegel", "The mirror shatters, and Spiegel steps out.")
    m("summoned.macht", "The ground turns to gold. Macht has arrived.")
    m("summoned.demon_king", "The sky darkens. The Demon King has returned.")
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
    for boss, lines in {
        "demon_king": ("The Demon King looks down at you. \"Another hero? How nostalgic.\"",
                       "The Black Sun rises behind the Demon King!",
                       "The Demon King stops holding back.",
                       "The Demon King falls. Somewhere, Himmel's party smiles."),
        "macht": ("Macht inclines his head politely. \"I would like to understand you. Hold still.\"",
                  "\"Let me show you my Golden Land.\"", "", "Macht crumbles into gold dust."),
        "linie": ("Linie watches you closely. \"I wonder whose fighting style you copied.\"",
                  "Linie mirrors your movements perfectly.", "", "Linie falls. She never had a style of her own."),
        "draht": ("Draht flexes his fingers. Threads glint in the air.", "Draht's wires fill the air around you.", "",
                  "Draht's threads go slack."),
        "lugner": ("Lügner smiles. \"I'm not here to fight. Truly.\"", "Lügner's wounds begin to close.", "",
                   "Lügner's last lie dies with him."),
    }.items():
        spawn, phase2, phase3, defeated = lines
        b(f"{boss}.spawn", spawn)
        b(f"{boss}.phase2", phase2)
        if phase3:
            b(f"{boss}.phase3", phase3)
        b(f"{boss}.defeated", defeated)
    b("mirror_frieren.spawn", "A perfect copy of Frieren steps out of the mirror.")
    b("mirror_frieren.guarded", "Her defensive magic holds. Wait for her to attack.")
    b("mirror_frieren.opening", "She attacked - her defence is down!")
    b("mirror_frieren.defeated", "The reflection shatters. Even a perfect copy has Frieren's weakness.")

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
    e("fail.left_arena", "you left the exam circle")
    e("trial.leaving", "You are leaving the exam circle! Go back or you fail.")
    e("trial.constructs", "Constructs broken: %s / %s")
    e("trial.wave", "Wave %s of %s!")
    e("trial.wave_progress", "Wave %s of %s - %s enemies left")
    e("trial.wave_cleared", "Wave cleared. Catch your breath...")
    e("trial.chest_found", "The genuine chest! Inside: your examination seal of approval.")
    e("trial.resumed", "Your exam resumes: %s")
    e("failed", "Exam failed: %s.")
    e("max_rank", "You hold the highest rank already.")
    e("not_eligible", "You are not yet eligible for the %s exam:")
    e("nothing_to_submit", "You have nothing the examiner is waiting for.")
    e("privilege", "Serie's privilege: choose one Legendary spell.")
    e("privilege_claimed", "Serie grants you %s.")
    e("privilege_none", "You have no privilege to claim.")
    e("progress", "Exam progress: %s / %s")
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
    s("journey.more_quests", "...and %s more")
    put(f"container.{NS}.pocket_space", "Pocket Space")
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
    s("spellbook", "Grimoire")
    s("spellbook.all", "All Spells")
    s("spellbook.empty", "Empty slot")
    s("spellbook.bind_hint", "Click to bind the chosen spell here")
    s("spellbook.hint", "Choose a spell from the left page to read it. Then click a slot below the book to bind it; right-click a slot to clear it.")
    s("spellbook.hint_empty", "Every spell you learn is written into this book.")
    s("spellbook.none", "You know no spells yet. Find a grimoire and read it.")
    s("spellbook.none_here", "You know no spells of this school yet.")
    s("spellbook.mana", "%s mana")
    s("spellbook.incant", "%s incantation")
    s("spellbook.channelled", "Channelled")
    s("spellbook.instant", "Instant")
    s("spellbook.casts", "%s casts")
    s("spellbook.mastery_level", "Mastery %s")
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
    "quest_auras_generals": ("The Three Who Served Aura", "Defeat Linie, Draht and Lügner"),
    "quest_the_golden_land": ("Not Everything That Glitters", "Defeat Macht of the Golden Land"),
    "quest_journeys_end": ("Journey's End", "Defeat the Demon King"),
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
