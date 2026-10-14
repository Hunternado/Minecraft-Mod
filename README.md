# Frieren: Mage's Journey

A Forge mod inspired by *Frieren: Beyond Journey's End*. You can train your mana, conceal it, release it, and sense
it in others. There are 62 data-driven spells, from everyday magic to the legends' own and the Demon King's, plus
the spells Serie creates for you alone. Casting works the way it does in the anime: incantations, charges, channels
and concentration a solid hit can break. There's also a 45-block building set in the anime's style, Association exams
that play out wherever you stand, demons who plead, lie and size you up by your mana, Übel, Frieren's party, and
multi-phase bosses up to the Demon King. Fan-made; all art and text are original.

| | |
|---|---|
| Minecraft | **26.2** |
| Forge | **65.1.0** |
| Java | **25** |
| Side | Client + server (required on both) |
| Version | **3.0.0**: the Magic Update |

> **Status:** builds against Minecraft 26.2 / Forge 65.1.0 and boots cleanly as a dedicated server and as a
> client. On every push, GitHub Actions also starts a real client. It plays a scripted showcase covering
> characters, spells, trials, bosses, the statue and the dungeons, and saves screenshots to the `ci-artifacts`
> branch. Balance and long multiplayer sessions still need human playtesting; see `docs/KNOWN_RISKS.md`.

## What's new in 3.0: the Magic Update

**Casting.** There's more than one way to cast now:
* **Instant** spells fire on the key press.
* **Incantations** wind up first.
* **Charged spells** fill while you hold the cast key and fire on release. The longer you hold, the stronger they
  hit: `0.2 + 0.8 x charge`. Mana pours in as they charge, and a full charge is what makes them lethal. Frieren's
  Zoltraak, Full-Power Zoltraak and Annihilation work this way.
* **Channels** keep going while you hold the key. Fern's Zoltraak works this way.
* **Toggles** are switched on and off: flight and defensive magic.

A bar under the crosshair shows each of these.

**Concentration.** A solid hit can break an incantation or a charge. Mastery, defensive magic and rank steady you.
A broken charge loses all the mana poured into it.

**Mastery** now really matters: a spell at full mastery hits 2.25 times as hard as a fresh one, and the grimoire
shows the multiplier.

**New rarity and school.**
* **Mythic** spells come from the Age of Myths and are for First-Class mages only.
* **Original Magic** is a new school (and a new ribbon in the grimoire) for spells made just for you.

**The legends' magic** (the Legendary tier used to be empty):
* **Frieren's Zoltraak**: a charged beam that pierces, cuts through barriers and hits demons half again as hard.
  At full charge it kills anything with up to 300 max health.
* **Fern's Zoltraak**: a ring of circles fires ever faster storms of beams. It does far more damage than the barrage.
* **Flamme's Deception**: for 30 seconds ordinary demons cannot target you, and your magic hits them 2.5 times as
  hard.
* **Heiter's Miracle**: heals everyone friendly nearby to full, cleanses every curse, and blesses them. The blessing
  refuses one death.
* **Frieren's Analysis**: shatters a demon's barrier, strips its buffs, and leaves it taking 40% more spell damage.
* **Übel's Reelseiden**: a cut that goes through armour and shields. Only Übel teaches it.

**The Demon King's magic** (Mythic; his grimoires drop when he dies):
* **Annihilation**: kills anything that isn't a boss and takes a quarter of a boss's health. It needs a fragile
  five-second charge.
* **The Black Sun**: three rings of telegraphed blasts.
* **Dominion**: every monster around obeys you.
* **Sovereign Ward**: you take a fifth of all damage, and half of every melee blow goes back to the attacker.

**Serie creates spells.** First-Class mages receive her **letter**, which calls her once a day.
1. Impress her: cast different spells near her. Rarer and better-mastered spells count for more, and releasing your
   limiter in front of her counts too.
2. Beat Übel.
3. Serie opens her **spell forge**. Choose the form, element and potency (up to *Annihilation*), plus reach, casting
   style, modifiers, colour and a name.

The forge shows every cost and trade-off before you commit. Annihilation, for example, costs 450 mana and a
five-second charge that a hit can break. You get three original spells, and asking again replaces one.

**Übel.** She turns up near Second-Class mages. Hit her and it's a duel, but she can't die in it. At a fifth of her
health she laughs, yields, and decides you're interesting:
* she gives you her Reelseiden as a grimoire, and her spare staff;
* she follows you everywhere, like a tamed wolf with opinions;
* she fights whatever threatens you;
* sneak and right-click to make her stay.

If she's "killed", she retreats and comes back the next day.

**Frieren and her party.**
* Frieren finally fights. Her Zoltraak does three times the damage to demons.
* A First-Class mage can sneak and talk to her to bring Frieren, Fern and Stark along.

**The Demon King** can only be summoned by a First-Class mage. Without Frieren, Fern *and* Stark beside you, his
Sovereign Barrier shrugs off 95% of every blow and his near-endless mana mends him. With them, the barrier breaks.
He fights with Annihilation (deal him 80 damage mid-charge to shatter it), the Black Sun, Dominion and Sovereign Ward.

**Frieren vs Aura.** If Frieren comes within four chunks of Aura, the anime scene plays out in full:
1. Aura sneers at Frieren's tiny mana.
2. She raises the scales.
3. Frieren releases her limiter.
4. "Aura, kill yourself."

**Mana you can read.**
* **Limiter release:** drop your concealment after hiding for 30 seconds or more and your mana floods out in a
  pillar. Weaker demons flee, and you're *Unleashed*: +20 to 40% spell power.
* **Demons judge you by your mana.** Ordinary demons run from a mage whose visible mana is six times their own, and
  treat a concealed one as prey. Casting while concealed flares your true mana for a moment.
* **Detection** draws a compass around the crosshair: markers by direction and strength ("weaker", "your equal",
  "overwhelming") and distance. Hidden signatures you noticed but couldn't see through show as a flickering "?".
  Trained mages also feel overwhelming mana nearby without asking.

**Also:**
* Azeryuze puppets take orders with the **Command Puppets** key (Z).
* Top-tier spells one-shot ordinary mobs and regular demons.
* The Stille tells you exactly how to tire it.
* Carried axes are strapped flat in leather loops instead of floating behind their owners.

## What's in 2.2

* **The whole cast, rebuilt from the anime.** Hair is soft and layered instead of a helmet of blocks: a rounded
  crown, a fringe that frames the face, side locks and tails that bend link by link. Faces are calm and
  symmetric; the smirks are gone. Thirteen characters were redrawn from reference art:
  * Frieren, Fern, Stark, Himmel, Aura and Macht;
  * Serie in her cream wrap top, mauve-banded sleeves and teal brooch, with hair to her knees;
  * Eisen the dwarf: horned helmet, a beard to his belly, round pauldrons and a rust-red cloak to the ground;
  * Heiter, new: combed-back green hair, glasses and a gold-edged black priest's robe;
  * Qual's grinning horned mask, fur mantle, stitched torso and hakama;
  * Linie's twin tails, bell sleeves and laced corset dress;
  * Draht's hidden eye, ram horns, ruffled shirt and braces;
  * Lügner's parted ash-blond hair, gem-pinned cravat, gold-trimmed coat and white boots.
* **The hero party at the statue.** Polish Himmel's statue to a shine and Eisen and Heiter appear beside his
  echo, the party together again for a moment.
* **A Demon King worth the name.** Tall and regal: long black hair, crimson-tipped horns sweeping back from a gold
  circlet, a high collar with pauldrons, a floor-length robe and a crimson-lined cape, and his own blade.
* **Weapons are carried like weapons.** Stark and Linie wear their axes strapped flat on the back, head over the
  shoulder. Himmel's sword hangs sheathed at his hip in a blue scabbard. Both are drawn only to fight.
* **A Frieren building set: 45 blocks** for towns, Association halls, elven ruins and demon castles:
  * whitewashed plaster and four half-timber patterns over dark timber beams;
  * terracotta, slate and verdigris roof tiles;
  * town cobblestone, Association stone (bricks, chiseled, gilded, pillar), elven ruin bricks (and cracked), demon
    castle bricks (and chiseled);
  * leaded glass and panes, mana lanterns, window flower boxes, grimoire shelves and the Association carpet;
  * stairs, slabs and walls for every brick, all craftable, with stonecutter recipes.
* **A creative tab for each part of the mod:** Magic, Gear, Building and Characters.
* **Thirteen everyday spells** that pull their weight in an ordinary world:
  * the spell to fell a tree in one stroke (living trees only: it will not fell your log cabin);
  * the spell to bless the fields (crops, saplings and berry bushes grow as if a season had passed);
  * the spell to find glittering stones (a chat report of nearby ores, with distance and direction);
  * the spell to carve a tunnel (a 3x3 passage the way you face; stone drops as if mined);
  * the spell to return home (a long incantation that takes you to your bed or the world spawn);
  * the spell to lay a bridge of light over gaps, ravines and lava, which fades after a minute;
  * five blessings shared with companions nearby: night sight, breathing underwater, working swiftly, falling
    like a feather and warding off flames;
  * the spell to smelt ore from your inventory, and a pocket of space that opens your ender chest anywhere.

  Block-breaking spells respect spawn protection and fire the usual break events, so claim mods apply.
* **A grimoire UI.** The spellbook is an open leather-bound book. The left page lists your spells by rarity, with
  silk ribbons to filter by school. The right page shows the chosen spell with its icon, lore, costs and
  mastery. Every spell has its own icon. The HUD, the Journey Journal, the examiner and the traders use the same
  parchment, ink and gold.

## What's in 2.1

* **The cast is drawn in the anime's style.** Skins use flat cel shading with soft, slightly violet shadows
  instead of noisy textures, hair falls in clean locks, and every face is hand-placed pixel art with big anime
  eyes. Frieren was rebuilt from her character sheet:
  * silver twin tails hanging past her waist, long ears with red drop earrings;
  * the white capelet with gold trim and red gems over her black-and-white striped high collar;
  * gold cuffs, dark tights and brown boots.
* **Characters hold real items.** Staffs and weapons are held in the hand like a player holds them, instead of
  being glued to the arm (the old staff ran straight through the shoulder). Casters aim their staff at you when
  they cast.
* **3D item models.** Every staff and weapon is a proper 3D model in the hand. Inventories, item frames and
  dropped items keep a crisp flat icon, and all 39 icons were redrawn.
* **New items:**
  * **Warrior's Axe** (Stark's), craftable;
  * **Replica of the Hero's Sword** (Himmel's), craftable;
  * **Blade of the Demon King**, dropped by the Demon King;
  * **Scale of Obedience**, dropped by Aura.

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
**frieren-mod-jar** artifact (a zip containing `frieren-2.2.0.jar`). Artifacts expire after 90 days.

**Building it yourself:** you need JDK 25 and internet access to `maven.minecraftforge.net`,
`piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net` and the Gradle plugin portal.

```bash
./gradlew build          # Windows: gradlew.bat build  ->  build/libs/frieren-2.2.0.jar
```

## Installing in CurseForge

1. In the CurseForge app, create a profile: **Minecraft 26.2**, modloader **Forge 65.1.0** (or newer 65.x).
2. Open the profile, click **⋯ → Open Folder**, and copy `frieren-2.2.0.jar` into `mods`.
3. For multiplayer, put the same jar in the server's `mods` folder.

## Playing

You start with a **Journey Journal**, which tracks your rank, mana, quests and exam eligibility. Read
**grimoires** to learn spells, bind them to quick slots in the **Spellbook** (pick a spell, then click a slot under
the book), then cast. Casting trains your mana capacity.
When you're eligible, talk to an **Association Examiner** to sit your next exam. The **Exam Seal** shows your
current trial and time left.

| Key (rebindable in Controls → Frieren: Magic) | Action |
|---|---|
| R | Cast the selected spell (hold to charge or channel; release to fire a charge) |
| V | Next spell slot |
| B | Spellbook |
| G | Toggle mana concealment |
| H | Sense mana around you |
| J | Journey Journal |
| Z | Command your Azeryuze puppets: look at a foe (attack), at the ground (guard), at nothing (follow); sneak + Z releases them |

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
  * `gen_held_items.py` builds the 3D in-hand item models; `preview_held.py` shows characters holding them through
    the game's exact hand transform chain.
  * `icons.py` holds the hand-drawn item icons that `gen_assets.py` saves.
  * `gen_building.py` builds the building set (textures, models, blockstates, loot, recipes and the Java registry).
  * `gen_gui.py` draws the GUI sprites and, through `gui_icons.py`, an icon for every spell.
  * `showcase/scenes.txt` is the in-game screenshot script CI runs.
  * `precheck.sh` runs the local checks before a push: syntax, duplicate locals, imports, call arity, static
    members against the 26.2 API dump, resources and creative tabs.

## License

All Rights Reserved (see `gradle.properties`). *Frieren: Beyond Journey's End* belongs to its creators; this is
an unofficial fan project.
