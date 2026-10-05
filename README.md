<p align="center">
  <a href="https://github.com/SlimefunNewHorizons/TinkersConstruct/tree/codex/port-1.21.11-neoforge">
    <img src="banner.svg" alt="Tinkers' Construct — DrakesCraft's unofficial porting project" width="100%">
  </a>
</p>

<h1 align="center">Tinkers' Construct · DrakesCraft Labs</h1>

<p align="center">
  <strong>Build tools your way. Shape them from the materials you discover.</strong><br>
  An unofficial community fork built on the work of SlimeKnights.
</p>

<p align="center">
  <a href="https://github.com/SlimeKnights/TinkersConstruct">Upstream project</a> ·
  <a href="https://slimeknights.github.io/docs/gameplay/tinkers-construct-3/">Gameplay guide</a> ·
  <a href="https://slimeknights.github.io/docs/">Developer documentation</a> ·
  <a href="modrinth-icon.svg">Modrinth icon artwork</a>
</p>

---

## About the mod

Tinkers' Construct is a tool-building mod about combining materials, crafting parts, and assembling tools that can be named, customized, repaired, and upgraded. Build tools in the Part Builder or work with metals in the multi-block Smeltery, then tailor them to the way you play.

This repository is the **DrakesCraft Labs organization fork** of [SlimeKnights/TinkersConstruct](https://github.com/SlimeKnights/TinkersConstruct). The default `1.20.1` branch preserves the existing Minecraft 1.20.1 / Forge codebase. The separate 1.21.11 port is a work in progress; it is not a release or a playable build.

## Project status

| Track | Target | Status |
| --- | --- | --- |
| Default branch: [`1.20.1`](https://github.com/SlimefunNewHorizons/TinkersConstruct/tree/1.20.1) | Minecraft 1.20.1 · Forge | Upstream-based source branch |
| [DrakesCraft port branch](https://github.com/SlimefunNewHorizons/TinkersConstruct/tree/codex/port-1.21.11-neoforge) | Minecraft 1.21.11 · NeoForge | Migration workspace; not release-ready |
| Mantle compatibility for 1.21.11 | Matching Minecraft / NeoForge target | Required before the dependent Tinkers migration can be completed |
| Mantle packaging inside Tinkers | Fewer separately installed files | Under investigation; no bundling decision is implemented |

The porting branch currently has build-workspace setup, but its source/API migration and matching Mantle dependency are incomplete. **Do not install or distribute artifacts from that branch.** Read the [1.21.11 porting roadmap](https://github.com/SlimefunNewHorizons/TinkersConstruct/blob/codex/port-1.21.11-neoforge/PORTING_1.21.11.md) for scope, milestones, and validation gates.

## Build from source

The default branch uses the Gradle wrapper and Java 17 toolchain configured in `build.gradle`.

```bash
./gradlew genIntellijRuns
./gradlew build
```

Import the repository as a Gradle project in IntelliJ IDEA and allow Gradle to finish setup before generating run configurations. The first build may download and prepare Minecraft/Forge dependencies. Do not add launcher credentials or other secrets to project files.

## Reporting an issue

Before opening an issue, search existing and closed reports. Include:

- Minecraft, Tinkers' Construct, Mantle, and Forge versions.
- Other potentially related mods and their versions.
- Clear reproduction steps and the expected versus actual result.
- For crashes, the crash report and relevant `latest.log` excerpt (remove account tokens, private server addresses, and other sensitive data first).
- Screenshots or a short recording when they help reproduce a visual or gameplay issue.

Use the repository's issue templates. Porting work should be reported against the 1.21.11 branch and clearly identify that it is an incomplete migration.

## Upstream support and releases

Upstream support policies apply to official SlimeKnights builds: modpack authors are responsible for support requests for their packs, and upstream does not support custom-built JARs or reports for outdated Minecraft versions. For upstream questions, use the [SlimeKnights Discord](https://discord.com/invite/njGrvuh) or the links on their [about page](https://slimeknights.github.io/about/). These contacts are not a support channel for DrakesCraft port builds.

JAR signatures from upstream build servers are informational only; follow the upstream project's warning not to verify signatures on its JARs using other mods.

---

## 📄 License & Upstream Attribution

This project is a sovereign fork maintained by [**JackStar6677-1**](https://github.com/JackStar6677-1) under [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons).

- **Original Project:** Created by the upstream authors and the open-source community.
- **DrakesCraft Optimizations:** Modernized for Paper/Purpur 1.21.11+, Java 21, high concurrency, asynchronous safety, and exploit/duplication prevention.
- **License:** Distributed under the original **GNU General Public License v3.0 (GPLv3)** (or original upstream license). See the [LICENSE](LICENSE) file for complete terms.
