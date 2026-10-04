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
