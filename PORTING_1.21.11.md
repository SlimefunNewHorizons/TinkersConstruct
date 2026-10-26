# Porting route: Minecraft 1.21.11 / NeoForge

This is the work plan and acceptance contract for the DrakesCraft fork. A green Gradle configuration is only the first gate; the port is not complete until the gameplay, resources, data, saves, and dedicated server have been exercised.

## Baseline and target

- Source baseline: upstream Tinkers' Construct 1.20.1, Forge 47.x (`1.20.1` branch).
- Target: Minecraft 1.21.11, NeoForge 21.11.x, Java 21, NeoGradle.
- Keep `tconstruct`, registry names, serialized data, and resource namespaces stable unless a migration is explicitly designed and tested.
- Upstream source and assets are MIT-licensed. Preserve upstream copyright/license notices and identify this as an unofficial port; do not imply upstream endorsement.
- Upstream's published FAQ currently says it does not authorize platform/version ports while the project remains actively maintained. Treat public binary distribution as gated on written upstream clarification/permission, even though local porting and this source fork are being prepared.

## Work packages and gates

### 0. Build foundation — MOSTLY COMPLETED

- [x] Establish official `1.21.11` default branch in `DrakesCraft-Labs/TinkersConstruct`.
- [x] Record this fork's scope, attribution, target and non-release status.
- [x] Verify wrapper/bootstrap with JDK 21 and official NeoForge MDK versions (`./gradlew tasks` succeeds).
- [x] Configure NeoGradle with NeoForge 21.11.45 and NeoForm `1.21.11-20251209.172050`. Note: `build.gradle` actually pins `net.neoforged.gradle.userdev` `7.1.39` (this doc previously said 7.0.198).
- [ ] Fix `META-INF/accesstransformer.cfg`: every entry below the `Identifier` lines still uses 1.20.1 SRG names (`f_20898_`, `m_21060_`, `m_7839_`, `func_82402_b`, ...). NeoForge 1.21 runs on Mojang names, so these never apply. Rewrite them with mojmap names or drop the ones no longer needed.
- [ ] Declare the access transformer in `META-INF/neoforge.mods.toml` (`[[accessTransformers]] file="META-INF/accesstransformer.cfg"`); since NeoForge 20.5 it is not picked up implicitly.

### 1. Mantle integration — SOURCE COPIED, NOT YET PORTED (Monorepo All-in-One)

The integrated Mantle tree comes from upstream Mantle's `1.21` (1.21.1 NeoForge) branch, not from a 1.21.11 port.

- [ ] Port Mantle from 1.21.1 to 1.21.11: 167 Mantle files still use `ResourceLocation` (renamed to `Identifier` in 1.21.11), plus the 1.21.2–1.21.11 API changes listed under section 2.

- [x] Merged Mantle source tree (594 Java files under `slimeknights.mantle.*`) directly into `src/main/java`.
- [x] Merged Mantle assets and resources directly into `src/main/resources/assets/mantle/`.
- [x] Configured `META-INF/neoforge.mods.toml` to declare both `[[mods]] modId="tconstruct"` and `[[mods]] modId="mantle"`.
- [x] Removed external Mantle compile dependency in `build.gradle` — mod builds as a self-contained all-in-one JAR.
- [x] Preserved full MIT license and copyright attribution for SlimeKnights.

### 2. Loader and core API migration — IN PROGRESS

- [x] Replaced ForgeGradle / Forge configurations with NeoGradle 7.x and NeoForge 21.11.x.
- [x] Configured `accesstransformer.cfg` with `public-f net.minecraft.resources.Identifier` and protected constructor for custom ID subclassing.
- [x] Migrated core ID and serialization utilities:
  - `IdParser.java`: ported to `Identifier`, `IdentifierException`, and `FriendlyByteBuf.readIdentifier()`.
  - `ResourceId.java`: adapted to `Identifier`.
  - `MaterialId.java`: adapted to `Identifier`.
  - `MaterialVariantId.java` & `MaterialVariantIdImpl.java`: adapted to `Identifier`.
  - `ModifierId.java`: adapted to `Identifier`.
  - `Util.java`: adapted to `Identifier`, thread-safe `Language.getInstance().has(...)`, `net.minecraft.util.Util.makeDescriptionId()`, and NeoForge `ICondition`.
- [ ] Migrate Forge registries (`RegistryObject`) to NeoForge `DeferredHolder` / `DeferredRegister`.
- [ ] Migrate Forge event buses (`MinecraftForge.EVENT_BUS` -> `NeoForge.EVENT_BUS`).
- [ ] Migrate Forge item/fluid handlers (`net.minecraftforge.items.*` -> `net.neoforged.neoforge.items.*`).
- [ ] Migrate item capabilities and NBT to 1.21.11 Data Components (`Equippable`, `Tool`, custom components).

Only the 7 core ID files above use `Identifier` so far. Audit of `origin/1.21.11` at `358b8a15df` (`git grep -l`, file counts, Tinkers = 1938 files under `slimeknights.tconstruct`; the build is not expected to compile yet):

| Remaining migration | Tinkers files |
|---|---|
| `net.minecraftforge` imports (Forge API not yet ported) | 570 |
| `ResourceLocation` -> `Identifier` | 503 (+167 in Mantle) |
| Item NBT (`CompoundTag`; `getTag`/`getOrCreateTag`/`setTag`) -> Data Components | 144 / 95 |
| JSON recipe serializers -> `MapCodec` / `StreamCodec` (`RecipeSerializer` / `JsonObject` usages) | ~100 / ~241 |
| Forge capabilities (`LazyOptional`, `ForgeCapabilities`, `ICapabilityProvider`) -> NeoForge capabilities | 62 |
| `ToolAction` -> `ItemAbility` | 40 |
| `MinecraftForge.EVENT_BUS` -> `NeoForge.EVENT_BUS` | 30 |
| `FMLJavaModLoadingContext` -> mod constructor `IEventBus` / `ModContainer` | 18 |
| `RegistryObject` -> `DeferredHolder` | 18 (+2 in Mantle) |
| `Tiers` / `TierSortingRegistry` -> 1.21.2+ `ToolMaterial` | 16 |
| `ArmorMaterial(s)` -> 1.21.2+ armor material / `Equippable` | 17 |
| `GuiGraphics` screens (1.21.6+ GUI render pipeline changes) | 79 |
| `ForgeConfigSpec` -> `ModConfigSpec` | 3 |

Minecraft 1.21.1 -> 1.21.11 changes not captured by the counts above:

- [ ] Data-driven enchantments (1.21) and their interaction with modifiers.
- [ ] Food and consumables as components (`Consumable`, `FoodProperties`, 1.21.2).
- [ ] `Equippable` component for armor/held equipment (1.21.2).
- [ ] Recipe inputs (`RecipeInput`) and recipe book/display changes (1.21–1.21.2).
- [ ] Entity render states (1.21.2) for custom entity renderers.
- [ ] Item model definitions under `assets/*/items/` (1.21.4); review custom/tool model loaders.
- [ ] NBT getters returning `Optional` (1.21.5) and `SavedData` codecs.
- [ ] `ValueInput` / `ValueOutput` for block entity and entity save/load (1.21.6).
- [ ] Networking via `CustomPacketPayload` + `StreamCodec` (Mantle network layer).
- [ ] Regenerate data with datagen: `src/generated/resources` still uses 1.20 folder names (`recipes`, `loot_tables`, `advancements`, `functions`, `tags/items`, `tags/blocks`, `tags/entity_types`); 1.21 uses singular names (`recipe`, `loot_table`, `advancement`, `function`, `tags/item`, `tags/block`, `tags/entity_type`). Remove stale `data/tconstruct/forge` paths.

### 3. Content and save compatibility

- [ ] Compare generated registries, recipes, tags, loot, advancements, item/block/entity definitions and language/model/texture assets against the source baseline.
- [ ] Add data migration/codec coverage for changed Minecraft serialization APIs.
- [ ] Test clean worlds and copies of representative 1.20.1 worlds. Never use production worlds for migration tests.
- [ ] Verify materials, tools, modifiers, stations, fluids, entities, structures, recipes and JEI/EMI-facing behavior.

### 4. Runtime, QA, and release gate

- [ ] `./gradlew clean check build` with JDK 21; retain logs and artifact hashes.
- [ ] Launch client and dedicated server, verify fresh startup/shutdown, world save/reopen, datagen and no new severe exceptions.
- [ ] Add automated tests for ported behavior and any migration code.
- [ ] Obtain upstream clarification before public binary distribution; publish port-specific release notes, source commit, dependencies, checksum, and known issues.
- [ ] Only then mark the port release-ready. No server deployment is included in this task.

## Mantle packaging resolution

The team selected and completed **Option 2: Source/Module Monorepo Integration**, following the proven pattern from DrakesCraft projects like *Fought* and *Core SF*:

* **Single Artifact:** Produces `TConstruct-1.21.11.jar` containing both Mantle and Tinkers.
* **Dual Runtime Declaration:** `META-INF/neoforge.mods.toml` contains independent mod entries for both `tconstruct` and `mantle`. NeoForge discovers both mods at launch, satisfying any third-party mod requirements without requiring an external Mantle JAR.
* **Unified Build Lifecycle:** Removes the need to maintain, synchronize, and publish an independent Mantle 1.21.11 repository or coordinate maven artifacts.

## Current progress and caveats

- The codebase is in early section 2 migration and does not compile yet; most Tinkers code is still on the 1.20.1 Forge API.
- Mantle source is integrated and internal references resolve locally, but it is still 1.21.1 code.
- Only the core identifier abstractions (7 files) have been upgraded to Minecraft 1.21.11's `Identifier`.
- Next batches involve migrating Forge registries (`RegistryObject` -> `DeferredHolder`) and event handlers.
- Work is being committed with author `JackStar6677-1 <pablo.elias.miranda.292003@gmail.com>` directly to `1.21.11`.
