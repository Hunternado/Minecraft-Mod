# Known Risks

This mod was written in a cloud session that could not download Minecraft or Forge artifacts (see
`docs/VALIDATION.md`), so **it has not been compiled against Minecraft yet**. Everything below is what an
offline audit could and could not prove, so that a compile error can be fixed in minutes instead of hunted for.

## What was verified offline

| Check | Tool | Result |
|---|---|---|
| Java syntax, all 153 files, JDK 25 | `tools/javac_parse.sh` | 0 errors |
| Internal consistency (calls between the mod's own classes: names, arity, types) | javac attribution with Minecraft missing; every remaining error was a member inherited from a missing Minecraft supertype | 0 mod-internal errors |
| Every `net.minecraft` / `net.minecraftforge` / `com.mojang` import exists in Forge 65.1, NeoForge 26.2 or Fabric API 26.2 sources, or in a class Forge patches | `tools/check_imports.py` | see table below |
| Resources ↔ code (models, textures, blockstates, loot, recipes, tags, structures, spells, sounds, every lang key, placeholder counts) | `tools/validate_resources.py` (mutation-tested: catches every injected fault) | 0 problems |
| Vanilla ids, tags and block states used by data and structure templates | 26.2 registry / data / block reports (misode/mcmeta) | all exist |
| Forge APIs (events, capabilities, networking, configs, key mappings, commands, biome modifiers, model `render_type`) | Forge 65.1 source, patches and test mods | matched |

## Vanilla APIs used without direct evidence in 26.2 reference code

These are long-standing vanilla members that the reference code bases simply never happen to call, so their exact
26.2 spelling could not be confirmed. Most have existed unchanged for many versions; if one fails to compile, the
fix is a rename at the listed call sites.

| API | Used in | If it fails |
|---|---|---|
| `ServerLevel.findNearestMapStructure(TagKey<Structure>, BlockPos, int, boolean)` | `ExamManager`, `SeekBehavior` | Same method under `/locate`'s implementation (`LocateCommand`) |
| `Entity.hurtMarked` (public field) | `SpellAreaEntity` (vortex pull) | Field that forces a motion sync for players |
| `Mob.xpReward` (protected field) | demon, boss, mimic constructors | Override `getBaseExperienceReward` instead |
| `Mob.registerGoals()` | all mobs | Name of the goal-setup hook in `Mob` |
| `Screen.isPauseScreen()` | all four screens | Remove the override (screens then pause single-player) |
| `CubeListBuilder.texOffs(int,int)`, `PartPose.offset(float,float,float)` | `FrierenHumanoidModel`, `MimicModel`, `StilleModel` | Builder names in `net.minecraft.client.model.geom.builders` |
| `Mob.setAggressive(boolean)` | `RangedSpellGoal` | `isAggressive()` **is** confirmed (Pillager patch); the setter is its pair |
| `PathNavigation.moveTo(...)`, `PathNavigation.stop()` | goals in `entity/ai` | Core navigation API |
| `new KeyMapping.Category(Identifier)` (record constructor) | `KeyBindings` | Forge explicitly sorts unregistered categories, so a public `register` factory would also work |

Vanilla AI goal classes with no 26.2 evidence (`LookAtPlayerGoal`, `RandomLookAroundGoal`,
`WaterAvoidingRandomStrollGoal`) were **removed** during the audit and replaced with the mod's own
`IdleLookGoal` / `WanderGoal`, built only on the confirmed `Goal` base class.

## Behavioural risks (compile fine, need an in-game look)

* **Structure placement height.** Surface templates use `start_height: -1` so their floor replaces the top
  ground block; on steep terrain `beard_thin` should smooth this, but the mage tower may sit partly buried on
  cliffs. Tune `start_height` in `tools/gen_data.py` if needed.
* **Kings' Tomb** is underground (y −30..10) with a ladder shaft that ends at the template's top. Players
  normally dig down from the Exam Seal's coordinates.
* **Placeholder art.** All textures are procedurally generated originals (`tools/gen_assets.py`) and sounds
  reuse vanilla sound events. They are deliberately simple and easy to replace.
* **Balance** has not been playtested. Every major number is a server config multiplier (see README).

## Fixed during the audit (for the record)

* All tag files were written without a `.json` extension. Minecraft would have ignored them, so no structure
  would ever generate, the exam objective lookups would return nothing, and armour/mining tags would be missing.
  `tools/gen_data.py` was fixed and `tools/validate_resources.py` now rejects any resource file Minecraft
  would not load.
* Structure-placed demons and Aura's thralls now become persistent (`finalizeSpawn` with
  `EntitySpawnReason.STRUCTURE`) instead of despawning like natural spawns.
* Lang text matched to actual formulas (artifact bonuses, hotbar-only artifacts) and exam timers now read
  `4:07` instead of `4:7`.
