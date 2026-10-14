# Phase 3 — Architecture

Package root: `com.hunternado.frieren` · mod id: `frieren`

```text
src/main/java/com/hunternado/frieren/
├── FrierenMod.java                 @Mod entry point: wires registries, configs, network, event handlers
├── FrierenIds.java                 Identifier helpers (frieren:<path>) and translation-key builder
├── registry/                       DeferredRegisters (items, blocks, entities, sounds, effects, data components,
│                                   creative tab, criterion trigger) — the only place content is registered
├── config/                         FrierenConfig (SERVER, balance) · FrierenClientConfig (HUD)
├── magic/                          MageRank, SpellRarity, SpellSchool (pure enums, no MC deps beyond Component)
├── data/                           Player capability: MagicData (model + NBT), SpellProgress, ExamProgress,
│                                   MagicDataProvider, MagicDataEvents (attach / clone / login / respawn / tick)
├── mana/                           ManaHelper (max mana, regen, training, concealment, detection),
│                                   ManaSignature (anything that has mana), ManaSensing (sense pulse)
├── spell/                          SpellDefinition (JSON codec) · SpellManager (reload listener + client copy)
│   │                               SpellBehavior · SpellBehaviors (behavior id → impl) · SpellCaster (server
│   │                               cast pipeline) · SpellContext · SpellDamage · SpellTargeting · SpellFx
│   └── behavior/                   One small class per behavior (beam, barrage, flame wave, flight, …)
├── combat/                         Damage types, SpellDamageSource, barrier absorption, obedience tracking
├── effect/                         Mob effects (enthralled, gilded, warded)
├── entity/
│   ├── demon/                      DemonEntity base (deceptive plea, regenerating barrier), soldier, mage, thrall
│   ├── boss/                       Aura (3 phases + Scales), Qual, Spiegel + mirror replicas
│   ├── npc/                        Frieren, Fern, Stark, Serie, examiner, priest, wandering mage, trades
│   ├── creature/                   Mimic, Stille
│   ├── magic/                      Plain-Entity spell constructs: mana bolt, spell area, mana decoy
│   └── ai/                         Goals built on the base Goal API only: ranged spell casting, hunt players
│                                   (respects concealment), hunt hostiles, follow leader, wander, idle look
├── item/  block/                   Item & block classes (behaviour only; registration lives in registry/)
├── advancement/                    MilestoneTrigger (one criterion trigger for all scripted milestones)
├── exam/                           ExamType definitions, ExamManager state machine, exam event hooks
├── quest/                          Quest definitions, QuestManager (counters → completion → rewards)
├── network/                        FrierenNetwork channel + packet/ (client→server intents, server→client sync)
├── command/                        /frieren admin command tree
├── event/                          ServerEvents (reload listener, datapack sync, commands, party spawns)
├── util/                           Messages, WorldRules, Hostility, EnumCodecs
└── client/                         Client-only (never referenced from common code paths)
    ├── ClientSetup.java            @EventBusSubscriber(value = Dist.CLIENT): renderers, layers, keys, HUD
    ├── ClientMagicCache.java       Client mirror of the local player's magic data
    ├── ClientPacketHandlers.java   Clientbound packet handling (only invoked from clientbound handlers)
    ├── ClientHooks.java            Screen openers called from item use on the client side
    ├── KeyBindings.java            KeyMapping definitions + per-tick key handling
    ├── gui/                        ManaHudLayer, SpellbookScreen, JourneyScreen, ExaminerScreen, TradeScreen
    └── render/                     Models (own LayerDefinitions), render states, renderers, ModRenderers

src/main/resources/
├── META-INF/mods.toml · pack.mcmeta
├── assets/frieren/                 items/, models/, blockstates/, textures/, equipment/, lang/en_us.json, sounds.json
└── data/frieren/                   frieren_spells/ (spell JSON), loot_table/, recipe/, advancement/, tags/,
                                    damage_type/, structure/*.nbt, worldgen/ (structures, sets, pools, ore),
                                    forge/biome_modifier/

tools/                              Python generators for every resource file + offline audit scripts
├── gen_data.py · gen_assets.py · gen_lang.py · gen_structures.py · pixelart.py
├── validate_resources.py           Self-contained resource ↔ code consistency check
├── check_imports.py                Import audit against reference sources (no Minecraft jars needed)
├── javac_parse.sh                  Parses every Java file with JDK 25 (syntax gate without Minecraft jars)
├── javac/DuplicateLocals.java      Flags locals redeclared in scope, which javac hides while types are unresolved
└── ci_probe.sh · ci_log_report.sh  CI helpers: javap API probe, filtered run-log report

.github/workflows/build.yml         CI: resource check, Gradle build (jar artifact), API probe, server + client boot
```

## Registration

* Every registry uses a Forge `DeferredRegister` created in `registry/*` and registered on the mod
  `BusGroup` in `FrierenMod`'s constructor. Items/blocks pass `Properties#setId(REGISTER.key(name))`
  (required since 1.21.2). Entity types are built with `EntityType.Builder#build(ENTITY_TYPES.key(name))`.
* Global-bus "registration-time" events (`EntityAttributeCreationEvent`, `SpawnPlacementRegisterEvent`,
  `EntityRenderersEvent.*`, `RegisterKeyMappingsEvent`, `AddGuiOverlayLayersEvent`) are subscribed through
  their static `BUS` fields; per-mod lifecycle events (`FMLCommonSetupEvent`) through `getBus(modBusGroup)`.
* Registry objects are referenced through `RegistryObject` constants — no string ids in gameplay code.

## Spells

* **Metadata** is data: `data/<ns>/frieren_spells/<id>.json` →
  `SpellDefinition` (Mojang `Codec`), loaded by `SpellManager` (a `SimpleJsonResourceReloadListener`
  added through `AddReloadListenerEvent`). Fields: `behavior`, `school`, `rarity`, `mana_cost`,
  `cooldown`, `cast_time`, `channel`, `params{...}`.
* **Behavior** is code: `SpellBehaviors` maps `frieren:<behavior>` ids to `SpellBehavior` implementations
  (`cast(SpellContext)`, optional `tickChannel`/`onToggle`). Unknown behavior ids are rejected at load
  with a log line instead of crashing.
* Definitions are synced to clients on login and `/reload` (`OnDatapackSyncEvent`) so tooltips and the
  spellbook show real costs, and so a datapack can rebalance a server without a client update.
* `SpellCaster` is the single server-side entry point: validation → mana/health cost → cooldown →
  (cast time) → behavior → mastery/training/quest hooks → sync.

## Player data

* `MagicData` is a Forge capability attached to every `Player` (`AttachCapabilitiesEvent.Entities`),
  serialized as NBT inside the entity's `ForgeCaps` tag, so it persists across relogs automatically.
* `PlayerEvent.Clone` copies everything (progression is never lost on death) after `reviveCaps()` on the
  old player; transient state (channels, flight, barrier) is reset; death fails an active exam.
* Server keeps a `dirty` flag + "last synced mana" so the network only carries changes:
  full snapshot packets on structural changes, a 12-byte mana packet at most every 10 ticks.

## Networking

`FrierenNetwork` builds one `SimpleChannel` (protocol version checked on both sides).

* Client → server (intents only): `CastSpell(slot)`, `ReleaseChannel`, `SelectSlot`, `BindSpell(slot,id)`,
  `ToggleConcealment`, `DetectMana`, `ExaminerAction(entityId, action)`.
* Server → client: `SyncMagicData` (full), `SyncMana`, `SyncSpells`, `ManaSense` (detection results),
  `OpenExaminer` (exam dialog payload).
* Every serverbound handler re-validates (known spell, distance to NPC, cooldowns, rank) — the client is
  never trusted with an outcome.

## Entities

* Living mobs extend vanilla `Monster` / `PathfinderMob` with vanilla `Goal`s registered in
  `registerGoals()` and attributes registered through `EntityAttributeCreationEvent`.
* Boss logic is phase-based (`AuraEntity.Phase`) and driven from `customServerAiStep`, using
  `ServerBossEvent` for the bar.
* Magic constructs (`ManaBoltEntity`, `SpellAreaEntity`, `ManaDecoyEntity`) extend plain `Entity` with
  explicit movement/collision so they don't depend on vanilla projectile internals; their visuals are
  client-side particles spawned in `tick()` when `level().isClientSide()`.
* Renderers: one generic humanoid renderer with its own `LayerDefinition` (no reliance on vanilla
  model-layer constants), texture chosen per entity type; small custom models for the Mimic and Stille;
  an empty renderer for particle-only constructs.

## World generation

* Structures are vanilla **jigsaw** structures defined in `worldgen/structure`, `structure_set`,
  `template_pool`; pieces are NBT templates in `data/frieren/structure/` generated reproducibly by
  `tools/gen_structures.py` (DataVersion 4903). Chest loot uses `LootTable` keys in block-entity NBT.
* Biome targeting uses `#frieren:has_structure/<name>` biome tags.
* Ore generation and mob spawns use Forge biome modifiers (`forge:add_features`, `forge:add_spawns`).

## Client/server separation

* Nothing in `client/` is referenced by common classes except inside lambdas that only execute on the
  client (clientbound packet handlers call `ClientPacketHandlers` from the handler body, so the class
  is never loaded on a dedicated server).
* `ClientSetup` is an `@Mod.EventBusSubscriber(value = Dist.CLIENT)` class: Forge never loads it on a
  dedicated server.
* Common code never touches `Minecraft`, screens, key mappings or renderers.

## 2.0 additions

### Character models (`client/render/character/`, `tools/models/`)
* **One model class for every character.** `CharacterModel` animates a fixed part tree (head with hair, twin
  tails, ears, horns and crown; body with jacket, cape and skirt; arms with props). It handles the walk cycle,
  breathing, cloth sway, the casting pose and the statue pose.
* **Generated geometry.** Each character's geometry is generated into `CharacterKind` by `tools/gen_models.py`
  from Python rigs in `tools/models/roster.py`. Skins are painted at 2× density into
  `textures/entity/character/`.
* **Offline previews.** `tools/preview_characters.py` renders the same rigs with Minecraft's transform order, so
  art can be judged without the game.
* **The mimic** is generated the same way by `tools/gen_mimic.py`, into `MimicLayer` plus its texture.
* **Anime style (2.1).** `roster.py` paints with `skinpaint`'s cel primitives under the classic names:
  * `cel_fill` gives flat colour with one shadow band, and `tone` hue-shifts shadows violet and highlights warm;
  * `anime_hair` draws clean locks with a highlight band;
  * `face_template` places each face by hand: a fringe of pointed locks, side locks, eyes from `EYE_STYLES`, brows
    and mouth.

  Frieren's front face is a fully custom pixel template.
* **Held items (2.1).** `CharacterModel` implements `ArmedModel` and `CharacterRenderer` adds the vanilla
  `ItemInHandLayer`:
  * `translateToHand` places items exactly like `PlayerModel` does (checked against the 26.2 bytecode).
  * While casting or swinging, the wrist cancels the arm's nominal raise and adds an aim tilt (55° for staffs,
    35° for weapons). Otherwise upright items would flip back over the shoulder.
  * `CharacterKind` carries each character's signature items. The renderer draws them only when the entity's
    hands are empty. They are visual only: nothing is equipped, synced or dropped.
  * Baked props remain for two cases: the statue's copper sword and Draht's wire spools.
* **3D item models (2.1).** `tools/gen_held_items.py` writes element models for staffs and weapons, and a
  `display_context` select so GUIs, frames and the ground keep the flat icon. Display transforms were designed with
  `tools/models/held.py`, the same hand transform chain in Python, and are previewed by `tools/preview_held.py`.
  Icons are hand-drawn pixel art in `tools/icons.py`.

### Magic visuals (`entity/fx/`, `client/fx/`, `client/render/fx/`)
* **Server-spawned visual entities.** `MagicCircleEntity`, `SpellBeamEntity` and `BarrierShellEntity` carry
  their look in synced data. They are `noSave` and discard themselves on reload.
* **Renderers.** Each entity draws emissive textured quads through `submitCustomGeometry`. Large ones override
  `shouldRender` with their real extent.
* **Particles.** `MagicFx` spawns tinted `MagicParticle`s from nine sprite families (`ModParticles`), driven by
  `SpellFxPacket`.

### Exam trials (`exam/TrialManager`)
* **Local arenas.** Each stage spawns its content around the examinee, inside a ground magic circle.
* **Lifetime.** Entities carry `frieren_trial` plus an owner tag. They are tracked in memory and discarded when
  the trial ends. Copies loaded from disk without a live trial are refused in `EntityJoinLevelEvent`.
* **Completion.** Deaths (from `LivingDeathEvent`) and a twice-a-second poll decide when a stage is done. The
  poll also replaces lost entities, enforces the arena radius, and rebuilds trials after a login.

### Bosses (`entity/boss/DemonBoss`)
* **Shared base.** It provides the boss bar, difficulty scaling, health phases, an attack cooldown, telegraphed
  ground blasts, beams and owned minions.
* **Subclasses** only implement `attack()`, plus optional `move()` and `tickBoss()`. They are the Demon King,
  Macht, Linie, Draht and Lügner.
* **Separate implementations.** `MirrorFrierenEntity`, `SpiegelEntity`, `AuraEntity` and `QualEntity` keep
  their own.

### Monuments (`entity/statue/`)
* **`HeroStatueEntity`** is a polishable, slowly re-oxidising copper Himmel, rendered by `HeroStatueRenderer`
  with one texture per patina stage.
* **`HimmelEchoEntity`** is the fleeting translucent echo that appears when a statue reaches full shine.

### Testing hooks
* **`/frieren cast`** runs `SpellCaster.adminCast`.
* **`/frieren exam trial`** runs `ExamManager.startStage`.
* **`/frieren fx`** runs `FxSampler`.
* **`client/dev/ShowcaseDirector`** is active only with `FRIEREN_SHOWCASE` set. It runs
  `tools/showcase/scenes.txt` in CI and takes the screenshots.

## 2.2 additions

### Rigs (`tools/models/characters.py`, `tools/models/parts.py`, `tools/models/roster.py`)
* **Nested part tree.** `PART_TREE` nests chains (`hair_back -> hair_back_2`, tails, locks, horns, cape and
  skirt), so `CharacterModel` bends hair and cloth link by link. `gen_models.py` walks the tree to emit the layer.
* **Soft hair.** `soft_head` builds a rounded crown, a fringe plate and an open-front shell on the hat layer with
  zigzag lock tips, instead of one inflated box. Every humanoid rig uses it.
* **Carried items.** `CharacterKind.carry()` (HAND, BACK or HIP) tells the renderer where the signature weapon
  rides while the character is not fighting; `CarriedItemLayer` draws it there in the NONE display context, and it
  moves to the hand once the character fights. Axes are strapped nearly upright on the back. Swords hang at the
  hip: their item definition selects a scabbard model for the NONE context, so no extra item exists.
  `tools/models/held.py` (`carry_matrix`) mirrors the layer for the previews.
* **Hero party echoes.** `HimmelEchoEntity` backs three entity types (Himmel, Eisen, Heiter);
  `HimmelEchoRenderer` takes the CharacterKind, and the statue calls `appearParty`.

### Building set (`tools/gen_building.py`, `registry/ModBuildingBlocks`)
* One table in `gen_building.py` describes all 45 blocks: family, shape, properties, texture painter and
  recipes. From it the script writes textures, blockstates (stairs, walls and panes from vanilla templates in
  `tools/building_templates/`), models, item definitions, loot, crafting and stonecutting recipes, tags, names
  and the Java registry `ModBuildingBlocks` with its own deferred registers.
* `ShapedDecorBlock` covers the one non-cube decoration (the window flower box).

### Creative tabs (`registry/ModCreativeTabs`)
* Four tabs (Magic, Gear, Building, Characters) with explicit item lists. `tools/check_tabs.py` fails precheck if
  an item is in no tab or in two.

### Everyday spells (`spell/behavior/`)
* `TreeFell`, `Growth`, `OreSense`, `Tunnel`, `Recall`, `Bridge`, `Buff`, `Smelt` and `PocketSpace` behaviours,
  configured by spell JSON params like every other spell.
* Block-breaking spells go through `ServerPlayerGameMode.destroyBlock` (tree felling) or check
  `Level.mayInteract` and `ForgeHooks.onBlockBreakEvent` per block (tunnel), so protection and claim mods apply.
* `Recall` uses `ServerPlayer.findRespawnPositionAndUseSpawnBlock(false, ...)`, vanilla's own respawn lookup,
  without spending an anchor charge.
* `ManaBridgeBlock` is a translucent walkable plate that schedules its own removal; it has no item and no loot.

### Grimoire UI (`client/gui/`)
* `GrimoireUi` draws the shared pieces from GUI atlas sprites (nine-slice where they stretch), generated by
  `tools/gen_gui.py`. `SpellIcons` (generated) says which spells have a drawn icon; others use a tinted rune.
* Screens draw their book or sheet in `extractBackground`, so widgets such as `InkButton` land on top of it.
* The showcase director gained `@screen` and `@bind` so CI can photograph the UI.

## 3.0 additions: the Magic Update

### Ways to cast (`spell/SpellCaster`, `spell/Concentration`)
* A spell's casting mode follows from its data: instant; `cast_time` (incantation); param `charge` (charged);
  `channel`; or a toggle behaviour. `SpellCaster` keeps the state in `MagicData` (pending cast, charge ticks and
  poured mana, channel) and reports every change to the owning client with `CastStatePacket`, which the HUD's
  cast bar animates locally.
* Charges pour `cost / chargeTicks` mana per tick while the key is held; release (the existing
  `ReleaseChannelPacket`) fires at power `0.2 + 0.8 * charge`. `SpellContext.charge()` and `executeCap()` let
  behaviours read it (executions need `execute_charge`, default 95%).
* `Concentration.onHurt` (from the hurt event) may break an incantation or charge; spells opt out with
  `steady` or weigh in with `fragility`.
* Power multipliers, in order: mastery (`masteryPower`: 1 + 1.25 * sqrt(f)), staff, config, charge, Unleashed.
  Damage multipliers live in `SpellDamage.hurt`: insignia, `demon_bonus`, Exposed, Deceiving ambush, executions.

### Rarities, schools and taught spells
* `SpellRarity.MYTHIC` (First Class) and `SpellSchool.ORIGINAL`. Param `taught` keeps a spell out of loot rolls
  and privileges (Übel's Reelseiden is given only by Übel). Unidentified grimoires prefer spells the reader lacks.

### Original magic (`spell/OriginalSpells`, `spell/SpellForge`, `spell/behavior/OriginalBehavior`)
* A recipe (form, element, potency, reach, casting, modifiers, colour, name) maps to `Stats` and to a
  `SpellDefinition` (behaviour `frieren:original`, school ORIGINAL, rarity MYTHIC, `customName`). The same code
  runs in the forge screen's preview and on the server, which recomputes everything from the sanitised recipe.
* Recipes are saved in the owner's `MagicData` with an id unique to player and slot
  (`frieren:original/<uuid>_<slot>`). The server registers them in `SpellManager`'s originals map at login and on
  creation; the client rebuilds its own from synced data. Lookups (`server(id)`, `client(id)`) check data-pack spells
  first, then originals; listings only ever contain data-pack spells.

### Mana reading (`mana/ManaSensing`, `mana/ManaHelper`, `entity/demon/DemonEntity`)
* Releasing concealment held 30+ s is a limiter release (Unleashed effect, frightened demons, pillar).
  Casting while concealed sets a 3-second mana flare (no concealment) unless the spell is `silent`.
* `DemonEntity.judgeMana` frightens ordinary demons facing six times their mana; Frightened demons drop their
  target and run. Bosses (`FrierenBoss`) never judge.
* `ManaSensePacket` carries per-entry flags (noticed, boss, seen through) and the detector's own mana; the HUD
  compass draws it. `passiveSense` warns trained mages of overwhelming signatures every two seconds.

### People (`entity/npc/`)
* `UbelEntity`: wild/duel/companion states in one class; the duel clamps damage so she yields at 20%; companion AI
  borrows the wolf's targeting (owner's attacker and target) and retreats instead of dying (`MagicData.ubelReturnAt`).
* `SerieEntity`: the appraisal (scored from `SpellCaster` and `ManaSensing` hooks) and the forge gate;
  `SerieLetterItem` summons her. `OpenSpellForgePacket` / `CreateSpellPacket` carry the forge round trip.
* `FrierenEntity`: combat (`RangedSpellGoal`), party leadership (her companions follow her, she follows the
  recruiting player), and `AuraDuel`, a server-ticked script that drives both entities through the scene.
* `FrierenNpcBase` keeps Frieren's party alive against bosses (they drop to 1 health instead of dying).

### The Demon King (`entity/boss/DemonKingEntity`)
* The Sovereign Barrier checks for Frieren, Fern and Stark within 40 blocks once a second; without them damage is
  cut to 5% and he regenerates. Annihilation tracks damage taken during its charge and breaks at 80.
