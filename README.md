# Tinkers' Construct — SlimefunNewHorizons port

Unofficial port of [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) by SlimeKnights to recent Minecraft versions on NeoForge.
Mantle is integrated into the same jar: no separate Mantle download is needed.

## Branches

Each Minecraft version has its own branch.

| Branch | Minecraft | NeoForge | Status |
| :--- | :---: | :---: | :--- |
| `1.21.1` | 1.21.1 | 21.1.241 | Builds and loads. Base of the newer ports |
| `1.21.11` | 1.21.11 | 21.11.48 | Port in progress, does not build yet |

## Contents

- Build custom tools, weapons and armor in the tool station from parts made of different materials.
- Each material gives its own durability, mining, attack and special traits; upgrades and abilities modify tools further.
- Melter, smeltery and foundry multiblocks, casting and alloying to process materials.
- Synchronized with the upstream 3.12 content: slimesuit with slime types, nautilus, horn, cheese and venom materials, edible tools and goat horn instruments. See [CHANGELOG](doc/CHANGELOG.md).

Compatibility alloys such as bronze or nicrosil only appear when the matching metal tags exist (`c:ingots/tin`, `c:ingots/nickel`, `c:ingots/chromium`, ...) or the `forceIntegrationMaterials` config option is enabled, same as upstream.

## Integrated Mantle

The jar declares two mods in `META-INF/neoforge.mods.toml`: `tconstruct` and `mantle`.
Mantle keeps its packages (`slimeknights.mantle.*`) and assets (`assets/mantle/`), so other mods depending on Mantle find it loaded.
Install the jar alone, plus JEI if you want recipe lookup.

## Building

Requires JDK 21.

```bash
./gradlew build
```

The jar is written to `build/libs/`. On the `1.21.11` branch, `./gradlew compileJava` shows the remaining porting errors.

## Credits and license

- [SlimeKnights](https://github.com/SlimeKnights): Tinkers' Construct and Mantle, MIT license.
- [zhuchuovo](https://github.com/zhuchuovo/TinkersConstruct-1.21.1): NeoForge 1.21.1 port of Tinkers' Construct and Mantle, MIT license, base of this fork.
- SlimefunNewHorizons: Mantle integration and the 1.21.11 port.

Distributed under the MIT license, see [LICENSE](LICENSE). This is an unofficial port: please report issues here, not to SlimeKnights.
