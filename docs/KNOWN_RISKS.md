# Known Risks

The mod **compiles against Minecraft 26.2 / Forge 65.1.0** and **boots cleanly as both a dedicated server and a
client**. On every push, `.github/workflows/build.yml` checks both on GitHub Actions and can drive a real client
through a scripted in-game showcase. The development sandbox cannot reach Forge's or Mojang's servers (see
`docs/VALIDATION.md`), so CI is where the real build and the in-game checks run. What remains is what only people
playing together can judge: feel, balance and long sessions.

## What is verified

| Check | Where | Result |
|---|---|---|
| Full Gradle build (`compileJava` reports every error in one pass) | CI `Build` | passes, produces `frieren-2.2.0.jar` |
| Dedicated server boot: every registry, config, network channel and datapack file | CI `Server smoke test` | clean |
| Client boot: model layers, renderers, HUD, every model / texture / item definition | CI `Client smoke test` | clean, no missing resources |
| In-game behaviour driven by `tools/showcase/scenes.txt` (commit tag `[showcase]`), screenshots published to the `ci-artifacts` branch | CI `In-game showcase` | see below |
| Resources ↔ code (models, textures, loot, tags, structures, spells, sounds, lang keys) | `tools/validate_resources.py` | 0 problems |
| Calls and constructors between mod classes match a declaration | `tools/check_calls.py` | 0 problems |
| No local redeclared inside its own scope (javac only reports this once types resolve) | `tools/javac/DuplicateLocals.java` | 0 problems |
| Exact signatures of every 26.2 API used | `javap` dumps of the real classes (CI `[api]`, `ci-artifacts/api/`) | looked up before use |
| Every `Vanilla.CONSTANT` and static call exists in 26.2; no `ItemStack` built in a static initializer | `tools/check_members.py` | 0 problems |
| Every item in exactly one creative tab; every spell has a grimoire icon | `tools/check_tabs.py`, `tools/validate_resources.py` | 0 problems |

What the showcase has confirmed in a running client (with screenshots):
* Every character renders with its own model and skin.
* The magic circles, beams, barrier shells and particle families render.
* The 16 new spells cast and hit (a demon soldier pleaded for its life mid-run).
* The practical and Mirror Frieren trials start, spawn their content and clean up. The trial log in CI records
  begin/end with tracked and defeated counts.
* The five new bosses spawn and fight (the Demon King summoned soldiers on cue).
* The statue plaza template places with its statue entity, and all four patina stages render.
* The new dungeons place.
* (2.2) All 19 rebuilt characters render alone in the portrait studio, with back-carried axes and held staffs.
* (2.2) The building set places: a half-timbered house with roof tiles, leaded glass, lanterns, walls and flower
  boxes, and a sampler of every family.
* (2.2) The everyday spells work in a running world: the field blessing ripens crops, ore finding reports the
  planted ores with correct counts, distances and directions, tunnel carving opens a 3x3 passage with drops, and
  the bridge of light spans a lava ravine.
* (2.2) The grimoire, the Journey Journal and the HUD render from the generated GUI sprites (nine-slice included)
  with every spell icon.

## Not covered by automation

* **Feel and balance.** Damage, mana costs, boss health and trial timings are first passes. Every major number is
  a spell JSON parameter or a server config multiplier (see README), so they can be tuned without code.
* **Multiplayer at scale.** Trials and bosses are per-player and friend-friendly by design: helpers count, and
  spectators are ignored by boss AI. They have only been exercised with one player in CI.
* **Every trial stage end-to-end.** The showcase covers the practical and replica stages. Survival waves, the
  grimoire hunt, Spiegel, Stille and Serie use the same trial framework, but nobody has played them to the
  finish in a running game yet.
* **Structure placement on rough terrain.** Templates use `start_height: -1` with `beard_thin`. On cliffs a
  structure can sit partly buried; tune per structure in `tools/gen_data.py`.
* **Sound.** Spells reuse vanilla sound events.
* **Everyday spells not photographed:** tree felling, return home, the five blessings, smelting and the pocket
  of space are covered by code review and the server boot only.
* **Examiner and trader screens** are restyled but not opened by the showcase (they need an NPC interaction).

## Bugs found by verification (for the record)

| Bug | Found by | Effect if shipped |
|---|---|---|
| All tag files written without `.json` | offline audit | no structure would ever generate; armour/mining tags missing |
| A helper inside an `@EventBusSubscriber` class | CI client smoke test | **client crashes during mod loading** |
| Literal `${...}` in a `mods.toml` comment | CI build | no jar |
| `Options.hideGui` / `Entity.hurtMarked` / `EntityType.IRON_GOLEM` (moved or renamed in 26.x) | CI compile, API dump | compile errors |
| Copied showcase world stopped on the backup prompt | showcase log | no screenshots; the director now confirms prompts |
| Big magic circles culled when their centre left the screen | showcase screenshot | arenas and area spells vanished from view |
| Spiegel copying spectators; spectators earning boss credit | showcase log | phantom replicas, free rewards |
| Spiegel, constructs, true chests and statues ignoring `/kill` | showcase log | admins could not clear them |
| Elf ears ending level with the top of the head | in-game screenshot vs preview | ears read as horns |
| Beam texture opaque only in its middle eighth | in-game screenshot | Zoltraak looked like a thread |
| A caster's magic circle snapping along on teleport | showcase screenshot | circles dragged through ender pearls and blinks |
| Structure searches (`findNearestMapStructure`) for exams and Seeking Magic | design review | could fail or stall far from structures; replaced by local trials and a loaded-chunk scan |
| Aura raising thralls on a timer with nobody to fight (a 1.x bug) | showcase screenshot | an idle Aura built up an army before players arrived |
| Himmel's echo placed relative to the statue, inside a wide plinth | showcase screenshot | the echo stood waist-deep in the steps |
| Polished-copper palette topping out at a skin tone | showcase screenshot | the gleaming statue read as a shirtless Himmel |
| "Defeat Linie, Draht and Lügner" counting any three general kills | code review | killing Linie three times passed the quest |
| Staffs and weapons baked into the arm model, pivoting in the hand | player report (screenshot) | Frieren's staff ran through her arm and out of her shoulder |
| An upright held item rotating with a raised arm | held-item preview (tools/preview_held.py) | casting would have flipped staffs back over the shoulder; the wrist now turns |
| Shadows as plain RGB multiplies on silver hair | player report (screenshot) | Frieren's hair read as dull grey |
| A local redeclared in a nested block of `AuraEntity.aiStep` | tools/javac/DuplicateLocals.java (written for it) | a compile error only CI could see |
| `BlockTags.COAL_ORES` and friends (26.2 ships them as data-only tags) | CI compile | ore sense and growth spells did not build; `check_members.py` written for it |
| A static `ItemStack` field in the tunnel spell | CI server smoke test | **the mod failed to load**: components are not bound at construction |
| Portrait studio killing each sitter | showcase screenshot | death poofs and loot in every portrait; sitters now drop into the void |
| The block sampler built where the mimic camera stands | showcase screenshot | the mimic close-up showed a wall of slate |
