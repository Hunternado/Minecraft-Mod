# Phase 2 — Game Design: *Frieren: Mage's Journey*

An unofficial fan expansion. All textures are original procedural placeholder art; all sounds reuse vanilla
sound events through `sounds.json` so they can be swapped by a resource pack.

## 1. Main gameplay loop

```
explore ─► find grimoires / ruins / demons ─► decipher grimoires (learn spells)
   ▲                                                    │
   │                                                    ▼
face named demons & bosses ◄─ rank up via exams ◄─ cast, train mana, raise mastery
```

1. **Explore** generated ruins (mage towers, forgotten libraries, demon camps, shrines, the Ruined King's Tomb).
2. **Decipher** grimoires found there. Your Continental Magic Association **rank** limits which rarities you
   can understand, so loot keeps pulling you toward exams.
3. **Train**: spending mana slowly raises your maximum mana (capped per rank); casting raises per‑spell
   mastery (cheaper, faster, stronger); living concealed raises concealment mastery.
4. **Certify** at an Association examiner. Each exam is an objective, not a recipe.
5. **Hunt demons**. Demons lie, shield themselves, conceal their mana and command thralls. Named demons are
   summoned at altars; Aura the Guillotine is the signature boss.

## 2. Mana

| Concept | Rule |
|---|---|
| Max mana | `base (config, 50) + growth + rank bonus + equipment bonus` |
| Growth | Every mana point spent adds training XP. XP needed per +1 growth rises with growth. Growth is capped by rank (Unranked 60 … First‑Class 600). Boss kills grant extra growth that ignores the cap (up to an absolute ceiling). |
| Regen | `0.04/tick × config × rank multiplier`; ×2 while **meditating** (sneaking and standing still); ×0.5 while concealing; paused for 2 s after casting. Mana regen is server-side and synced at most every 10 ticks. |
| Concealment (key) | Toggle. Your *apparent* mana signature becomes `true × (1 − concealment%)`. Demons' detection range against you shrinks by the same factor (they lose track of you). Time spent concealed raises **concealment mastery** (0–100). |
| Detection (key) | 3 s cooldown pulse, radius 24 (+rank). Lists nearby mana signatures (apparent, not true) and outlines them with client-side particles. Mimics and concealed demons read low — unless your detection beats their concealment. |
| Meditation | Sneak still for regen ×2 and a small concealment training bonus. |

**Signature mechanic — Aura's Scales (Azeryuze).** In phase 2 Aura weighs the nearest player's mana against
her own (scaled by boss difficulty). Apparent mana is used for the *start* of the weighing — if you were
concealing and **release concealment during the 5‑second weighing**, your true mana is boosted by your
concealment mastery (up to +100%). Win and Aura is forced to obey ("Aura, kill yourself" — she takes a huge
self-inflicted hit and is stunned). Lose and you are *Enthralled* (rooted, silenced, weakened). This is the
mechanical payoff for the whole concealment system.

## 3. Spells

Spells are **data-driven** (`data/<ns>/frieren_spells/*.json`, reloadable, datapack-overridable): cost,
cooldown, cast/channel time, school, rarity, rank requirement, mastery curve and numeric parameters. Each
JSON names a **code behavior** that implements the effect, so new spells can be added by datapack by
recombining behaviors with new parameters.

Rarities: `common`, `uncommon`, `rare`, `ancient`, `legendary`, `demon`, `forbidden`.

* Rank gates the rarity you can learn (Unranked: common · Fifth: uncommon · Fourth: rare · Second: ancient · First: legendary).
* **Demon** spells are human analyses of demon magic: demon grimoires must be analysed at a **Spell Research Desk** (Third‑Class+), mirroring how humans analysed Qual's Zoltraak.
* **Forbidden** spells need First‑Class and also cost health.

### Spell list (23)

| Spell | School | Rarity | Behavior (summary) |
|---|---|---|---|
| Zoltraak | offensive | common | Instant mana beam (hitscan). The baseline of human combat magic. Absorbed by barriers. |
| Zoltraak Barrage (Fern) | offensive | rare | Channelled: a Zoltraak every 4 ticks with spread while held. |
| Qual's Original Zoltraak | offensive | demon | Piercing beam: passes through every target and ignores armour, but modern defensive magic stops it cold (×3 barrier absorption). |
| Vollzanbel (Hellfire) | offensive | rare | Cone of hellfire; ignites, only burns blocks if spell griefing is enabled. |
| Judradjim (Lightning) | offensive | rare | Calls lightning at the aimed point (visual bolt + controlled AoE damage, no fire grief). |
| Waldgose (Tornado) | offensive | rare | Spawns a tornado that drags and lifts mobs, then flings them. |
| Catastravia | offensive | ancient | Rains light arrows over an area for 3 s. Anti-army magic. |
| Reelseiden (Übel) | offensive | rare | Short arc that "cuts anything": bonus damage against barriers and shields. |
| Diagolze (Macht) | offensive | forbidden | Curse of gold: petrifies a living target into a gilded statue for a time, or turns stone into worthless Cursed Gold. |
| Defensive Magic | defensive | common | Toggle hexagonal barrier: absorbs damage by spending mana (projectiles fully, melee 70%). Shatters at 0 mana. |
| Barrier Dome | defensive | uncommon | 8 s dome: deflects hostile projectiles, grants allies Resistance. |
| Goddess Healing | defensive | uncommon | Priest magic: heals the aimed ally (or yourself). |
| Curse Purification | defensive | uncommon | Cleanses harmful effects and Enthrallment from you and nearby allies. |
| Flight Magic | utility | uncommon | Toggle survival flight; drains mana per second; graceful slow-fall when mana runs out. |
| Flower Field (Flamme) | utility | common | Blooms flowers on grass around you; allies inside regenerate. Frieren's favourite spell. |
| Mimic Detection | utility | common | Reveals mimics nearby — with the canonical 99% accuracy. |
| Clothes Cleaning (Fern's privilege) | utility | uncommon | Repairs your worn equipment a little at a time. |
| Rust Removal | utility | common | De-oxidises copper around the aimed block (polish the Hero's statue). |
| Seeking Magic | utility | uncommon | Points to your last death location, otherwise to the nearest mod ruin. |
| Light Orb | utility | common | Conjures a light that fades after a few minutes. |
| Gathering Magic | utility | common | Pulls nearby dropped items and XP to you. |
| Azeryuze (Scales of Obedience) | special | demon | Weighs your mana against a target's vitality; on success it fights for you for 60 s. |
| Land's Doppelgänger | special | rare | Leaves a mana decoy that draws aggro for 10 s. |

Plus the two core abilities (keys, not spells): **Mana Detection** and **Mana Concealment**.

### Casting

* Five quick slots. Keys: *cast*, *next/previous slot*, *spellbook*, *conceal*, *detect*, *journal* (all
  rebindable via Forge key mappings).
* Pipeline (server-authoritative): known? → rank/rarity OK? → off cooldown? → enough mana (and health for
  forbidden)? → cast time (movement-slowed wind-up) → behavior → mastery gain → sync.
* **Staffs** reduce mana cost and cooldown and amplify power. Casting bare-handed costs +40% mana.
* **Mastery** 0–100: up to −30% cost, −25% cooldown, +50% power.

## 4. Progression — Continental Magic Association ranks

| Rank | Requirements to sit the exam | Exam |
|---|---|---|
| Fifth‑Class | know 2 spells, max mana ≥ 60 | **Practical** — defeat 5 hostile mobs with spells within 10 minutes |
| Fourth‑Class | know 4 spells, one spell at mastery 25 | **Survival** — defeat 8 monsters including 2 demons within 15 minutes |
| Third‑Class | know 6 spells incl. a defensive spell | **Retrieval** — bring back a *Sealed Examination Grimoire* from a Forgotten Library (the Exam Seal points the way) |
| Second‑Class | know 9 spells, two at mastery 50 | **Labyrinth** — enter the Ruined King's Tomb and defeat Spiegel at the Mirror Altar |
| First‑Class | know 12 spells, max mana ≥ 250, Second-Class | Three stages: **(1)** capture a Stille bird with a Stille Cage · **(2)** defeat Spiegel again under time pressure · **(3)** Serie's interview: use Mana Detection on Serie while she conceals; your detection must see through her (detection power from rank, concealment and detection mastery). |

Promotion rewards: rank title, regen multiplier, higher growth cap, and a reward grimoire. First‑Class also
grants **Serie's Privilege**: one guaranteed Legendary grimoire of your choice at the examiner.

Exams are per-player state, so any number of players can sit exams at once. Spiegel credit goes to every
examinee within 48 blocks when it dies. Dying fails the current exam (it can be retaken).

## 5. Enemies

| Entity | Tier | Mechanics |
|---|---|---|
| Demon Soldier | 1 | Melee. **Deception**: at low health it pleads in human words and stops fighting; if you stop attacking it strikes back for bonus damage. |
| Demon Mage | 2 | Keeps distance, fires demon bolts, **mana barrier** that absorbs damage and regenerates out of combat, conceals its mana. |
| Headless Thrall | 2 | Aura's obedient armoured corpse soldiers. Slow, tanky, knockback resistant. |
| Mimic | trap | Looks like a chest in ruins. Open it and it eats you ("It's dark! I'm scared!"). Mimic Detection reveals it. Drops good loot. |
| Qual the Corruption | mini-boss | Sealed demon, summoned at a Sealed Shrine altar. Fires the original Zoltraak, teleports. Drops the demon grimoire of Qual's Zoltraak. |
| Spiegel | exam boss | Hides behind mirror replicas of nearby examinees (copying their gear, firing Zoltraak). Exposed only while all replicas are down. |
| **Aura the Guillotine** | boss | Phase 1 (100–60%): commands thralls, demon bolts. Phase 2 (60–30%): **Scales of Obedience** (see §2). Phase 3 (<30%): blink-strikes, guillotine sweep AoE, continuous thralls. Boss bar, configurable health/damage. |

All demons dissolve into mana when killed (no corpse), dropping demon horns / demonic mana cores and
occasionally demon grimoires.

## 6. NPCs

| NPC | Role |
|---|---|
| Wandering Mage | Vanilla trading UI: grimoires, staffs, mana potions for emeralds / mana crystals. |
| Association Examiner | Exam hub: shows requirements, starts/abandons exams, hands out exam tools, grants Serie's privilege. Lives in Association Outposts. |
| Priest of the Goddess | Trades holy-magic grimoires and heals players on interaction (cooldown). |
| Frieren | Rare traveller. Trades the "useless" everyday spells she collects. Travels with Fern and Stark. |
| Fern / Stark | Frieren's companions: Fern casts Zoltraak at hostiles, Stark fights in melee. Never attack players. |
| Serie | Appears at the examiner for First-Class stage 3. Conceals her mana; interview logic. |

All NPCs use vanilla goals (no per-tick scanning beyond the vanilla target selectors).

## 7. Structures (jigsaw, data-driven)

| Structure | Where | Contents |
|---|---|---|
| Ruined Mage Tower | overworld forests/plains/meadows | grimoire chest (common–rare), mana crystals |
| Forgotten Library | overworld, partially buried | 2 chests, mimic, demon mage guards, exam grimoire target |
| Demon Camp | plains/savanna/taiga | demon soldiers + mage, demon loot |
| Association Outpost | plains/meadow/forest | Association Examiner, wandering mage chance |
| Hero's Statue | plains/meadow | oxidised copper statue of Himmel, Blue Moon Weed, quest |
| Sealed Shrine | mountains/taiga | Summoning Altar (Qual), sealing loot |
| Ruined King's Tomb | underground | Mirror Altar (Spiegel), rare/ancient loot |
| Aura's Ruined Fortress | plains/savanna (rare) | Summoning Altar (Aura), thralls, legendary loot |

Spacing/separation live in `worldgen/structure_set/*.json`; server admins tune frequency with a datapack
override (documented in README) because structure sets load before mod configs.

## 8. Items & blocks (each has a job)

* **Grimoires** (one item per rarity tier; the spell lives in a data component). Read to learn.
* **Demon Grimoire** — must be analysed at the Spell Research Desk.
* **Staffs**: Apprentice Staff → Mage's Staff → Frieren's-style **Sage's Staff** → **Guillotine Staff** (Aura's blade). Cost/cooldown/power modifiers.
* **Mage's Attire** (4 armour pieces): +max mana and regen per piece, set bonus.
* **Mana Crystal** (ore + block): crafting, analysis fuel, potion base.
* **Mana Potion**, **Heiter's Flask** (big mana, nausea — he was always drunk).
* **Artifacts** (work from hotbar/offhand): *Mirror-Lotus Ring* (regen, Himmel quest reward), *Concealment Charm* (concealment training ×2, signature −15%), *Demon Hunter's Insignia* (+20% spell damage to demons).
* **Exam Seal** (points to exam objectives), **Stille Cage** (catch the Stille), **Journey Journal** (opens UI).
* **Summoning Sigils**: *Sigil of the Guillotine* (Aura), *Seal of Corruption* (Qual), *Mirror Shard* (Spiegel).
* Drops: **Demon Horn**, **Demonic Mana Core**, **Guillotine Blade**.
* Blocks: Mana Crystal Ore (+deepslate), Mana Crystal Block, Spell Research Desk, Summoning Altar, Blue Moon Weed, Cursed Gold Block, Mana Light (spell-only).

## 9. Quests (Journey Log)

Auto-tracked, shown in the Journey Journal; completion grants rewards:

* *The Journey Begins* — learn your first spell → Apprentice Staff.
* *A Field of Flowers* — cast Flower Field 10 times → Blue Moon Weed seeds.
* *The Hero's Statue* — use Rust Removal near the Hero's Statue → Mirror‑Lotus Ring.
* *Mimic Hunter* — reveal 3 mimics (getting eaten counts as research) → rare grimoire.
* *Spell Collector* — learn 10 spells → mana growth.
* *Demon Slayer* — defeat 25 demons → Demon Hunter's Insignia.
* *Aura, Kill Yourself* — defeat Aura → huge mana growth + legendary grimoire.

## 10. Multiplayer

* Server is authoritative for mana, cooldowns, learning, casting, exams and quests; clients only send
  intents (cast slot N, bind spell X to slot N, toggle concealment …) which are validated.
* Clients receive: full magic snapshot on login/respawn/dimension change/datapack reload and after
  structural changes; compact mana updates at most every 10 ticks and only when changed.
* Spell visuals use vanilla particle/sound packets (`ServerLevel#sendParticles`, `playSound`) — no per-tick
  custom FX packets.
* Exams, quests and boss credit are per-player; bosses use `ServerBossEvent` shown to nearby players.
