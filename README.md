# Frieren: Mage's Journey

A Forge mod inspired by *Frieren: Beyond Journey's End*. You can train your mana, conceal it, and sense it in
others. There are 39 data-driven spells, Continental Magic Association exams that play out wherever you stand,
demons who plead and lie, and multi-phase bosses up to the Demon King. Fan-made; all art and text are original.

| | |
|---|---|
| Minecraft | **26.2** |
| Forge | **65.1.0** |
| Java | **25** |
| Side | Client + server (required on both) |
| Version | **2.0.0** |

> **Status:** builds against Minecraft 26.2 / Forge 65.1.0 and boots cleanly as a dedicated server and as a
> client. On every push, GitHub Actions also starts a real client. It plays a scripted showcase covering
> characters, spells, trials, bosses, the statue and the dungeons, and saves screenshots to the `ci-artifacts`
> branch. Balance and long multiplayer sessions still need human playtesting; see `docs/KNOWN_RISKS.md`.

## What's in 2.0

* **Characters who look like themselves.** Frieren (silver twin tails, elf ears, staff with the red orb), Fern,
  Stark, Serie, Himmel, Eisen, Aura, Qual, Linie, Draht, Lügner, Macht and the Demon King each have their own
  model. Hair, twin tails and capes sway, and casters lift their staff with their hair rising in the mana flow.
* **Magic you can see.** Rotating magic circles in four styles (arcane, holy, demonic, floral) appear in front of
  casters and under area spells. Zoltraak fires as a layered beam. Barriers are hexagonal shells that flash when
  hit. Nine families of tinted particles cover sparks, runes, petals, frost, embers and demon wisps.
* **39 spells** (16 new), including:
  * Offensive: Frost Lance, Earth Spikes, Water Whip, Zoltraak: Full Power and Mana Release.
  * Control and defence: Sorganeil (hold someone in your gaze), a Reflection Barrier, Goddess's Sanctuary and
    Serie's Bottled Golem.
  * Demon grimoire magic: Lügner's Blood Blades and Draht's Wire Snare.
  * Frieren's beloved everyday spells: harvesting, cooking, clearing the skies, Wind Step, and the spell to turn
    sour grapes sweet.
* **Exams without treasure hunts.** Every exam stage is a local trial inside a magic circle around you, with
  nothing to search for and no structures to find:
  * Fifth Class: break drifting exam constructs with spells.
  * Fourth Class: hold the circle against three waves of Aura's soldiers.
  * Third Class: a grimoire hunt where one chest is real and the rest are mimics. Mimic Detection helps.
  * Second Class: Spiegel's mirror.
  * First Class: catch the Stille, duel **Frieren's reflection**, then pass Serie's interview. The reflection's
    defence drops only when she attacks, just like in the anime.

  Trials clean up after themselves on pass, failure, death, logout or a server restart. The exam clock pauses
  while you are offline. Friends can jump in and help.
* **Bosses:**
  * **The Demon King** has three phases: dark Zoltraak, crimson rain, the Black Sun, and a final blink.
  * **Macht of the Golden Land** casts Diagolze rays that gild whoever they hit.
  * **Aura's generals** Linie, Draht and Lügner guard her fortress until they fall.

  The 1.x bosses (Aura, Qual, Spiegel) are still here.
* **Himmel's statue** is Himmel himself, cast in copper with his sword raised. Over in-game days it oxidises from
  polished copper through exposed and weathered to verdigris. The spell to remove rust from bronze statues
  polishes it back one stage per cast, and something lovely happens when it gleams.
* **Dungeons:** the Golden Land ruins and the Demon King's castle join the mage tower, forgotten library, demon
  camp, association outpost, sealed shrine, Kings' Tomb and Aura's fortress.
* **A proper mimic.** Disguised, it is an ordinary chest. Revealed, it shows teeth, fangs and a tongue, and hops
  after you.

## Getting the jar

**Without building:** open the repository's **Actions** tab and pick the latest green **Build** run. Download the
**frieren-mod-jar** artifact (a zip containing `frieren-2.0.0.jar`). Artifacts expire after 90 days.

**Building it yourself:** you need JDK 25 and internet access to `maven.minecraftforge.net`,
`piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net` and the Gradle plugin portal.

```bash
./gradlew build          # Windows: gradlew.bat build  ->  build/libs/frieren-2.0.0.jar
```

## Installing in CurseForge

1. In the CurseForge app, create a profile: **Minecraft 26.2**, modloader **Forge 65.1.0** (or newer 65.x).
2. Open the profile, click **⋯ → Open Folder**, and copy `frieren-2.0.0.jar` into `mods`.
3. For multiplayer, put the same jar in the server's `mods` folder.

## Playing

You start with a **Journey Journal**, which tracks your rank, mana, quests and exam eligibility. Read
**grimoires** to learn spells, assign them in the **Spellbook**, then cast. Casting trains your mana capacity.
When you're eligible, talk to an **Association Examiner** to sit your next exam. The **Exam Seal** shows your
current trial and time left.

| Key (rebindable in Controls → Frieren: Magic) | Action |
|---|---|
| R | Cast the selected spell (hold to channel) |
| V | Next spell slot |
| B | Spellbook |
| G | Toggle mana concealment |
| H | Sense mana around you |
| J | Journey Journal |

To summon a boss, use its sigil on a **Summoning Altar**. Altars appear in boss structures and can be crafted.
* **Seal of the Golden Land** (Macht): found in the Golden Land, or crafted from cursed gold and demonic cores.
* **Seal of the Demon King**: crafted from Aura's Guillotine Blade, Spiegel's mirror shards, demonic cores and a
  nether star.

## Configuration

`<world>/serverconfig/frieren-server.toml` (copy it into `defaultconfigs/` to apply to new worlds) has:
* mana: `baseMaxMana`, `manaRegenMultiplier`, `manaGrowthMultiplier`, `masteryGainMultiplier`;
* spells: `spellDamageMultiplier`, `spellManaCostMultiplier`, `spellCooldownMultiplier`, `allowSpellGriefing`
  (default off, and also needs `mobGriefing`), `flightMagicEnabled`;
* bosses and exams: `bossHealthMultiplier`, `bossDamageMultiplier`, `examTimeMultiplier`, `demonDeception`.

`config/frieren-client.toml` sets HUD visibility and position. Structure frequency and biomes are data-pack
overridable (`worldgen/structure_set/`, `tags/worldgen/biome/has_structure/`).

## Commands (operators, permission level 2)

```
/frieren mana get|fill|set|growth <player> ...
/frieren spell give|remove|learnall|list|mastery <player> ...
/frieren cast <player> <spell>          cast any spell instantly (testing, showing off to friends)
/frieren rank get|set <player> ...
/frieren exam start|abandon|pass <player>
/frieren exam trial <player> <stage>    jump straight into a trial: practical, survival, retrieval, labyrinth,
                                        stille, replica, interview
/frieren quest complete|reset <player> <quest>
/frieren fx <pos> | fx particles <pos>  lay out every magic visual (art review)
/frieren cooldowns [player]
/frieren reset <player>
/frieren debug [player]
```

## Project layout

* `docs/`: `DESIGN.md` (game design), `ARCHITECTURE.md` (code structure), `VALIDATION.md` (how the build and the
  game are checked) and `KNOWN_RISKS.md` (what is still unverified).
* `tools/` generates every resource from Python:
  * `gen_models.py` builds the character models and skins from `models/roster.py`.
  * `gen_mimic.py` builds the mimic.
  * `gen_fx.py` builds the particles, magic circles, beams and shells.
  * `gen_structures.py`, `gen_data.py`, `gen_lang.py` and `gen_assets.py` cover the rest.
  * `preview_characters.py` renders turnaround sheets without the game.
  * `showcase/scenes.txt` is the in-game screenshot script CI runs.
  * `precheck.sh` runs the local checks (syntax, duplicate locals, imports, call arity, resources) before a push.

## License

All Rights Reserved (see `gradle.properties`). *Frieren: Beyond Journey's End* belongs to its creators; this is
an unofficial fan project.
