# Better Bundles

Fabric mod for Minecraft **26.1** that upgrades vanilla bundles into practical mini-containers: a custom **2×2 slot panel** opened by hovering over a bundle in any supported container grid.

## Compatibility

| Component     | Version           |
|---------------|-------------------|
| Minecraft     | 26.1              |
| Fabric Loader | 0.19.3+           |
| Fabric API    | 0.145.1+26.1      |
| Java          | 25+               |

## Build

From this directory:

```bash
./gradlew build
```

The output JAR is at `build/libs/better_bundles-1.0.0.jar` (version from `gradle.properties`).

## Test

```bash
./gradlew test          # unit tests (JUnit + Fabric Loader bootstrap)
./gradlew runGameTest   # server GameTests
./gradlew build         # full CI gate (includes both)
```

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.1.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.1 and place it in your mods folder.
3. Copy the built JAR into your Minecraft `mods` folder.

## Branch Policy

Each supported Minecraft version has its own long-lived branch named after the exact `minecraft_version` in `gradle.properties`. Version branches are the source of truth; `main` is not used.

- **Default branch:** `26.1` (also `1.21.11` for the prior game release)
- **Feature/fix work:** branch from the target version as `{version}/feature-name`, merge back into the version branch

```bash
git clone https://github.com/spicato-minecraft/better_bundles.git
cd better_bundles
git checkout 26.1
```

## Releases and Modrinth

Merging code does **not** publish to Modrinth. Publishing happens only when a GitHub Release is created.

Release tags use `{minecraft_version}-{mod_version}` (e.g. `1.21.11-1.0.0`), matching `gradle.properties`.

1. Merge your changes into the version branch (e.g. `1.21.11`).
2. Bump `mod_version` in `gradle.properties` if shipping new player-facing changes.
3. Add a changelog at `releases/{tag}.md` — see `releases/TEMPLATE.md`.
4. Create a GitHub Release with tag `{minecraft_version}-{mod_version}` targeting the version branch.

See [minecraft-mods/docs/README.md](../docs/README.md) for org-wide release conventions.

## Test Strategy

| Layer      | Purpose                                              |
|------------|------------------------------------------------------|
| Unit tests | Pure logic (`BundleInventory`, `BundleOverlayPlacement`) |
| GameTests  | In-world bundle storage, migration, nesting rules    |

Behavior tests are tracked in [Notion Tasks](https://app.notion.com/p/3b716e8d4da380adbd93cf0a73b39d5c). Manual UAT items live in the Mod Catalog.

## Architecture

Server-authoritative 4-slot bundle inventory stored as a data component on bundle items. Client-side grid-aligned hover overlay for direct drag-and-drop access. Vanilla right-click pick/insert cycling preserved across inner slots 0→3.

Implementation roadmap: [Implementation roadmap](https://app.notion.com/p/3bf16e8d4da38148ab81f19f6c9fb3e1).
