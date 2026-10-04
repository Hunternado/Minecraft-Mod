# Known Risks

The mod **compiles against Minecraft 26.2 / Forge 65.1.0** and **boots cleanly as both a dedicated server and a
client**. Both are checked on every push by `.github/workflows/build.yml` on GitHub Actions. The development
sandbox itself cannot reach Forge's or Mojang's servers (see `docs/VALIDATION.md`), so CI is where the real build
runs. What remains is gameplay behaviour that only a person in a world can judge.

## What is verified

| Check | Where | Result |
|---|---|---|
| Full Gradle build (ForgeGradle 7, Minecraft decompiled + Forge patched, `compileJava` reporting every error) | CI `Build` | passes, produces `frieren-1.0.0.jar` |
| Dedicated server boot: mod construction, every registry, configs, network channel, every datapack file (spells, loot, recipes, advancements, tags, damage types, worldgen) | CI `Server smoke test` (`runGameTestServer`) | clean, no errors or mod warnings |
| Client boot: client mod construction, key mappings, model layers, entity renderers, HUD layer, every model / blockstate / texture / item definition | CI `Client smoke test` (`runClient` under xvfb) | clean, no crash, no missing-resource warnings |
| Resources ↔ code (models, textures, loot, tags, structures, spells, sounds, every lang key, placeholder counts) | `tools/validate_resources.py`, also in CI | 0 problems |
| Exact signatures of APIs that no reference code used (`findNearestMapStructure`, `Hud#isHidden`, …) | CI `Probe Minecraft API signatures` (`javap` on the real 26.2 classes) | confirmed |

## Not covered by automation

* **Gameplay.** No automated test enters a world, so spell feel, AI behaviour, boss phases, exams and quests are
  only checked by reading the code. A crash in an entity's tick or a spell behaviour would only show up in play.
  The next step would be Forge GameTests that spawn each entity and cast each spell.
* **Structure templates** are decoded and validated offline (and the Kings' Tomb maze is path-checked), but they
  are only loaded by the game when a structure generates.
* **Structure placement height.** Surface templates use `start_height: -1` so their floor replaces the top
  ground block. On steep terrain `beard_thin` should smooth this, but the mage tower may sit partly buried on
  cliffs. Tune `start_height` in `tools/gen_data.py` if needed.
* **Kings' Tomb** is underground (y −30..10) with a ladder shaft that ends at the template's top. Players
  normally dig down from the Exam Seal's coordinates.
* **Placeholder art and sound.** Textures are procedurally generated originals (`tools/gen_assets.py`); sounds
  reuse vanilla sound events.
* **Balance** has not been playtested. Every major number is a server config multiplier (see README).

## Bugs found by verification (for the record)

| Bug | Found by | Effect if shipped |
|---|---|---|
| All tag files written without `.json` | offline audit | Minecraft ignores them: no structure would ever generate, exam objectives find nothing, armour/mining tags missing |
| `ClientSetup.humanoid(RegisterRenderers, …)` helper inside the `@EventBusSubscriber` class | CI client smoke test | EventBus 7 rejects it: **client crashes during mod loading** for every player |
| Literal `${...}` in a `mods.toml` comment | CI build | Gradle's resource templating fails; no jar |
| `Options.hideGui` (removed in 26.x) | CI compile | compile error; the HUD now sits in Forge's pre-sleep stack, which hides with F1 |
| `Entity.hurtMarked` (renamed `needsSync` in 26.x) | Mekanism 26.2 sources | compile error |
| Structure-placed demons/thralls despawning | offline review | camps and Aura's fortress empty out; now persistent via `finalizeSpawn(STRUCTURE)` |
| Three vanilla AI goals with no 26.2 evidence | offline audit | replaced with the mod's own `IdleLookGoal` / `WanderGoal` |
