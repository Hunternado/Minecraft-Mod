# Frieren: Mage's Journey

A Forge mod inspired by *Frieren: Beyond Journey's End*: mana you can train and conceal, a data-driven spell
system built around Zoltraak, Continental Magic Association exams, deceptive demons, and multi-phase bosses
(Aura the Guillotine, Qual, Spiegel). Fan-made; all art and text are original placeholders.

| | |
|---|---|
| Minecraft | **26.2** |
| Forge | **65.1.0** |
| Java | **25** |
| Side | Client + server (required on both) |

> **Status:** builds against Minecraft 26.2 / Forge 65.1.0, and boots cleanly as a dedicated server and as a
> client. GitHub Actions checks all three on every push. Gameplay itself has not been playtested yet; see
> `docs/KNOWN_RISKS.md`.

## Getting the jar

**Without building:** open the repository's **Actions** tab and pick the latest green **Build** run. Download the
**frieren-mod-jar** artifact (a zip containing `frieren-1.0.0.jar`). Artifacts expire after 90 days.

## Building the jar yourself

Requirements: JDK 25 (`java -version` must say 25) and internet access to `maven.minecraftforge.net`,
`piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net` and the Gradle plugin portal.

```bash
./gradlew build          # Windows: gradlew.bat build
```

The first build downloads and sets up Minecraft + Forge (several minutes). The mod jar is:

```
build/libs/frieren-1.0.0.jar
```

Use that file. Ignore any `-sources` jar if one appears.

Useful dev tasks: `./gradlew runClient`, `./gradlew runServer`.

## Installing in CurseForge

1. In the CurseForge app, create a profile: **Minecraft 26.2**, modloader **Forge 65.1.0** (or newer 65.x).
2. Open the profile, click **⋯ → Open Folder**.
3. Copy `frieren-1.0.0.jar` into the profile's `mods` folder.
4. Play. For multiplayer, put the same jar in the server's `mods` folder.

## Playing

You start with a **Journey Journal** (rank, mana, quests, exam eligibility). Read **grimoires** to learn spells,
assign them in the **Spellbook**, then cast. Casting trains your mana capacity. Passing exams raises your rank,
which raises your growth cap and unlocks rarer spells.

| Key (rebindable in Controls → Frieren: Magic) | Action |
|---|---|
| R | Cast selected spell (hold to channel / keep a barrage going) |
| V | Next spell slot |
| B | Spellbook |
| G | Toggle mana concealment |
| H | Sense mana around you |
| J | Journey Journal |

* **23 spells** across offensive, defensive, utility and special schools, in 7 rarities (Common → Legendary,
  plus Demon and Forbidden). Spells are JSON in `data/frieren/frieren_spells/`, so a data pack can rebalance or
  add spells that reuse existing behaviours.
* **Exams** at Association Outposts: Fifth through First Class, with stages such as the practical, survival,
  grimoire retrieval from a Forgotten Library, the labyrinth under the Kings' Tomb, catching a Stille, and
  Serie's interview.
* **Demons** speak, plead and lie. Concealing your mana keeps them from noticing you. **Aura** weighs her mana
  against yours, so release your concealment at the right moment.
* **Structures:** mage tower, forgotten library, demon camp, association outpost, hero's statue, sealed
  shrine, kings' tomb, Aura's fortress. Summon bosses at their altars with the matching sigil.

## Configuration

`<world>/serverconfig/frieren-server.toml` (per world; copy it into `defaultconfigs/` to apply to new worlds): `baseMaxMana`, `manaRegenMultiplier`,
`manaGrowthMultiplier`, `masteryGainMultiplier`, `spellDamageMultiplier`, `spellManaCostMultiplier`,
`spellCooldownMultiplier`, `allowSpellGriefing` (default off; also needs `mobGriefing`), `flightMagicEnabled`,
`bossHealthMultiplier`, `bossDamageMultiplier`, `examTimeMultiplier`, `demonDeception`.

`config/frieren-client.toml`: HUD visibility and position.

Structure frequency: override `data/frieren/worldgen/structure_set/<name>.json` (`spacing` / `separation`) in a
data pack. Biomes per structure: `data/frieren/tags/worldgen/biome/has_structure/<name>.json`.

## Commands (operators, permission level 2)

```
/frieren mana get|fill|set|growth <player> ...
/frieren spell give|remove|learnall|list|mastery <player> ...
/frieren rank get|set <player> ...
/frieren exam start|abandon|pass <player>
/frieren quest complete|reset <player> <quest>
/frieren cooldowns [player]
/frieren reset <player>
/frieren debug [player]
```

## Project layout

* `docs/VALIDATION.md` covers version validation and the environment limitation. `docs/DESIGN.md` is the game
  design. `docs/ARCHITECTURE.md` explains the code structure. `docs/KNOWN_RISKS.md` lists what is unverified.
* `tools/` holds Python generators for every resource (`gen_data.py`, `gen_assets.py`, `gen_lang.py`,
  `gen_structures.py`) and the audit scripts (`validate_resources.py`, `check_imports.py`, `javac_parse.sh`).
  Regenerate with `python3 tools/gen_<x>.py`, then run `python3 tools/validate_resources.py`.

## License

All Rights Reserved (see `gradle.properties`). *Frieren: Beyond Journey's End* belongs to its creators; this is
an unofficial fan project.
