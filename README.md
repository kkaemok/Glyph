# Glyph

A high-performance HUD engine for Paper and Velocity, based on BetterHud.

Glyph is an independent downstream fork of [BetterHud dev](https://github.com/toxicity188/BetterHud/tree/dev), with full Git history and MIT attribution preserved. Initial targets: Paper 26.1.x, 26.2, 26.3, Java 25 and Velocity. This is a development fork; live-client rendering and production load validation are still required.

## Build

Open this directory as a Gradle project in IntelliJ IDEA and select a Java 25 Gradle JVM. Run `gradlew.bat build` on Windows or `./gradlew build` elsewhere. The wrapper supplies Gradle; any compatible Java 25 vendor works.

Paper and Velocity distributions are written to `build/libs` (`Glyph-paper` and `Glyph-velocity`). `pluginJar` and `velocityJar` build individual distributions. `:glyph-core:test` runs portable logic tests. First-time NMS setup downloads matching Paper development bundles and can take time.

Fabric and Minecraft 1.21 implementations are intentionally removed. Private libraries are bundled at build time. Glyph does not inject URLs into server classloaders, publish to BetterHud registries or report to BetterHud metrics IDs.

## Native state

Use `GlyphAPI.inst().playerManager.getHudPlayer(uuid)` to obtain a HUD player. Its `hudState` accepts immutable `HudValue.Number`, `Boolean`, `Text`, `RichText` and `Identifier` values. `setAll` publishes related values atomically.

Patterns support `[state_number:health]`, `[state_boolean:alive]` and `[state:name]`. Text is escaped as literal MiniMessage content; RichText preserves an Adventure component. Existing placeholders remain supported and each source is sampled once per player/event during a render frame.

A layout can declare `dependencies: [health, alive]` to cache its rendered segment until those native inputs change. Declare **all** changing inputs. Leave this absent for legacy polling placeholders, player-following content and animations. An empty list declares a static layout. Force-update bypasses the cache. Unchanged final HUD output skips compaction, conversion and packets even with legacy polling.

## Screen effects

On Paper 26.3, call `GlyphAPI.inst().postEffects(player)` on the player's owning thread. Use `add`, `remove`, `clear`, `set` and `values`. Check `supported()` on older platforms. Effects must exist in the client's pack; Glyph uses Paper's API without executing commands.

Conflicting pack assets, including `minecraft:end_of_frame`, require explicit resolution rather than silent overwrite.

## Layouts and images

Glyph adds rows with measured widths, compiled columns and stacks alongside
absolute layouts. Image directories can provide an entire selectable icon
library; dynamic patterns use bounded candidates and a fallback. Nine-slice
sources generate named size variants with fixed corners and tiled/stretched
edges. See [configuration examples](GLYPH_CONFIGURATION.md).

## Documentation

- [Upstream review and sync policy](GLYPH_UPSTREAM_NOTES.md)
- [Architecture](GLYPH_ARCHITECTURE.md)
- [Validation and profiling](GLYPH_PERFORMANCE.md)
- [Migration from BetterHud](GLYPH_MIGRATION.md)
- [Configuration extensions](GLYPH_CONFIGURATION.md)
- [Original requirements](GLYPH_REQUIREMENTS.md)

## Attribution

BetterHud was created by toxicity188 and its contributors. Glyph retains and extends their renderer, layouts, packs, head cache, integrations, popup/compass behavior and bossbar interception. See [LICENSE](LICENSE) and upstream history.
