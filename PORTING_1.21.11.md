# Porting Tinkers' Construct to Minecraft 1.21.11

Base: zhuchuovo's NeoForge 1.21.1 port (branch `1.21.1`), with Mantle integrated into the same jar.
Target: Minecraft 1.21.11, NeoForge 21.11.48, ModDevGradle 2.0.148, Java 21.

## Status

| Area | State |
| --- | --- |
| Common code (Mantle + Tinkers) | Compiles |
| Dedicated / game test server | Starts, loads all data without errors, game test passes |
| Data (recipes, tinkering, Mantle data) | Converted to the 1.21.2+ formats |
| Client code (models, renderers, screens, book) | Not ported, excluded from the build |
| JEI integration | Not ported, excluded from the build |
| Item client definitions (`assets/*/items/*.json`) | Missing, required since 1.21.4 |
| Datagen providers | Excluded, generated data is converted in place |

The client is excluded by the `PORT-STAGE` block in `build.gradle`. Remove it once the client packages are ported.

## How the data was converted

`scripts/port/convert_ingredients.py` rewrites recipe, tinkering and Mantle data:

- `{"item": "x"}` becomes `"x"`, `{"tag": "x"}` becomes `"#x"`.
- Custom ingredients use `neoforge:ingredient_type` instead of `type`.
- Sized ingredients become `{"ingredient": ..., "amount_needed": n}`.
- `neoforge:item_exists` becomes `neoforge:registered`, `neoforge:true`/`false` become `always`/`never`.

Other renames: `minecraft:chain` is `minecraft:iron_chain`, attributes lost their `generic.`/`player.` prefix,
and the removed `#minecraft:trim_templates` tag is replaced by the list of vanilla templates.

## Known shortcuts

Each has a `shortcut:` comment in code where applicable.

- **Effect cures**: NeoForge removed `EffectCure` in 21.5. Milk immunity is kept through `NoMilkEffect#isMilkImmune` and a mixin.
  Per instance cure items (skull and fluid effects) are gone, so those effects last until they expire.
- **Shields**: blocking and shield disabling moved to the `BLOCKS_ATTACKS` and `WEAPON` components. Tinkers keeps its own
  `shield_block` ability, but tools still need those components set when stats rebuild.
- **Tool data verification on load**: `verifyComponentsAfterLoad` no longer exists, so stats are not rebuilt when a world loads.
- **Recipe removal command**: input filters only see recipes with placement info.
- **Spawn egg colors**: spawn eggs use textures since 1.21.5, the color arguments are ignored.
- **Fluid transfer and item capabilities**: legacy `IFluidHandler`/`IItemHandler`/`IEnergyStorage` implementations are adapted to
  the transfer API through Mantle's `transfer` package instead of being rewritten.
- **Loot modifiers for modded ores**: modifiers referencing ore tags that no mod defines fail to decode with a warning.
- **Compostables**: `ComposterBlock.COMPOSTABLES` is ignored by NeoForge, a `neoforge:compostables` data map is still needed.

## Remaining work

1. Client port: Mantle models (connected, retextured, item layer, colored block, NBT key, fallback), Tinkers models
   (tool, material, tank, fluid container), block entity renderers, screens, the book, armor models and client extensions
   (see `shared/client/PendingClientExtensions.java`).
2. Client item definitions for every item.
3. JEI plugin.
4. Datagen providers.
5. A client run to verify rendering and GUIs.
