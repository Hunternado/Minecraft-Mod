# Phase 1 — Technical Validation

Target environment, as requested and **not substituted**:

| Component | Required | Verified against | Result |
|---|---|---|---|
| Minecraft Java | 26.2 | Forge `minecraft.versions.toml` at tag `65.1` (`minecraft = "26.2"`), mcmeta `26.2` version.json | ✅ exists, released 2026‑06‑16 |
| Forge | 65.1.0 | Forge git tag `65.1` (commit `d17cfd0b`, "26.2 RB 1", 2026‑07‑27). Forge versions are `<tag>.<offset>`, so 65.1.0 is exactly that tag | ✅ exists |
| Java | 25 | Forge `minecraft.versions.toml` (`java = "25"`), MDK `java.toolchain.languageVersion = 25` | ✅ required by MC 26.1+ |
| Gradle | wrapper from MDK | Forge `mdkGradleWrapper` task → Gradle **9.5.0** | ✅ wrapper copied verbatim |
| ForgeGradle | from MDK | MDK `id 'net.minecraftforge.gradle' version '[7.0.17,8)'` (Gradle Plugin Portal) | ✅ |
| EventBus | from MDK | Forge `settings.gradle` → `eventbus 7.0.5` (+ `eventbus-validator` annotation processor) | ✅ |
| Mappings | — | Minecraft 26.x ships **unobfuscated**; ForgeGradle 7 "mavenizer" uses official names. No MCP/SRG/Parchment config needed | ✅ |
| Data version / pack formats | — | mcmeta `26.2`: data version **4903**, data pack **107.1**, resource pack **88.0** | ✅ used for NBT + pack.mcmeta |

## API conventions confirmed for this version (from Forge 65.1 source + patches)

These differ from most tutorials and were all checked against real 26.2 code:

* `ResourceLocation` → **`net.minecraft.resources.Identifier`** (`Identifier.fromNamespaceAndPath`, `ResourceKey::identifier`).
* `GuiGraphics` → **`GuiGraphicsExtractor`**; `Screen.render` → **`extractRenderState(GuiGraphicsExtractor,int,int,float)`**; text via `text(...)` / `centeredText(...)`; screens open with **`minecraft.gui.setScreen(...)`**, current screen `minecraft.gui.screen()`.
* HUD layers: `AddGuiOverlayLayersEvent` → `ForgeLayeredDraw` (`ForgeLayer#extract`).
* EventBus 7: events expose a static `BUS` (`TickEvent.PlayerTickEvent.Post.BUS.addListener(...)`); mod-bus events use `Event.getBus(modBusGroup)`; mod constructor receives `FMLJavaModLoadingContext`.
* Vanilla entity type constants live in **`net.minecraft.world.entity.EntityTypes`** (not `EntityType.X`).
* Entity persistence uses **`ValueInput` / `ValueOutput`** (`addAdditionalSaveData(ValueOutput)`), damage entry point **`hurtServer(ServerLevel, DamageSource, float)`**.
* `ServerBossEvent` constructor now takes a **`UUID`** first.
* `KeyMapping(String, InputConstants.Type, int, KeyMapping.Category)`, categories are `new KeyMapping.Category(Identifier)`.
* Commands: `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`; `IdentifierArgument`.
* Registration: Forge `DeferredRegister` + `RegistryObject`; items/blocks need `Properties#setId(REGISTER.key(name))`; `EntityType.Builder#build(ResourceKey)`.
* Capabilities still exist in Forge 65 (`@AutoRegisterCapability`, `AttachCapabilitiesEvent.Entities`, `ICapabilitySerializable<CompoundTag>`). Entities serialize them under `ForgeCaps`; dead players' caps are invalidated before `PlayerEvent.Clone`, so the clone handler calls `reviveCaps()`/`invalidateCaps()`.
* Networking: `ChannelBuilder.named(...).simpleChannel().play().clientbound().addMain(...).serverbound().addMain(...).build()`; `PacketDistributor.PLAYER.with(player)`.
* Data paths: `loot_table/`, `recipe/`, `advancement/`, `tags/item/`, `structure/` (singular), Forge biome modifiers in `data/<ns>/forge/biome_modifier/`.
* Package moves seen in 26.x: `npc.villager.AbstractVillager`, `projectile.arrow.AbstractArrow`, `projectile.hurtingprojectile.*`, `monster.illager.*`, `net.minecraft.util.Util`.

## Environment limitation discovered in this cloud session (not a version incompatibility)

The session's outbound network policy **blocks** `maven.minecraftforge.net`, `piston-meta.mojang.com`,
`piston-data.mojang.com` and `libraries.minecraft.net`. ForgeGradle needs these to download and
"mavenize" Minecraft + Forge, so `gradlew build` **could not complete inside this session**. Gradle itself,
the Gradle Plugin Portal (ForgeGradle 7.0.40) and Maven Central are reachable.

Mitigation used while writing the code:

1. Forge 65.1 source, its vanilla patches and test mods were cloned from GitHub (allowed) and used as the
   reference for every Forge API.
2. NeoForge 26.2.x and Fabric API 26.2 sources (both compiled against the same unobfuscated 26.2 code) were
   used as additional evidence of vanilla method/class names.
3. misode/mcmeta 26.2 data/asset dumps were used for all JSON formats, registry ids and data versions.
4. `tools/check_imports.py` cross-checks every `net.minecraft.*` import in this project against the classes
   actually referenced by those code bases; `tools/validate_resources.py` validates every JSON file and every
   model/texture/loot/structure reference; `tools/javac_parse.sh` parses every Java file with JDK 25.

Anything that could not be verified this way is listed in `docs/KNOWN_RISKS.md` so a compile error can be
fixed in minutes rather than hunted for.

## Resolution: building on GitHub Actions

The network limitation was worked around by running the real build on GitHub's runners, which have
unrestricted internet access. `.github/workflows/build.yml` runs these steps on every push:

1. `tools/validate_resources.py`.
2. `./gradlew build`, with javac set to report every error in one pass. The mod jar is uploaded as the
   `frieren-mod-jar` artifact.
3. `tools/ci_probe.sh`, which prints `javap` signatures from the real 26.2 classes for any API listed in
   `tools/ci_probe.txt`.
4. `runGameTestServer`, a dedicated-server boot that loads all registries and datapacks.
5. `runClient` under xvfb, which boots the client to the title screen and fails on any crash report.

Open-source mods with 26.2 branches (JEI, Jade, Sodium, Lithium, Iris, Mekanism, Curios, Waystones,
Biomes O' Plenty, SmartBrainLib, GeckoLib) were also used as an API reference corpus.

