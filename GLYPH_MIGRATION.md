# Migrating from BetterHud

Stop the server and retain your existing BetterHud files. Install Glyph in place of BetterHud; running both HUD interceptors together is unsupported. Copy HUD, layout, text, image, popup, compass, background and player-data configuration to `plugins/Glyph`.

- New default namespace: `glyph`; an existing `namespace: betterhud` remains valid. New output defaults to `Glyph/build`. Review the old `build-folder-location`.
- Plugin ID is Glyph / `glyph`. Permissions use `glyph.*`; update grants. Commands retain their HUD semantics.
- Public packages are `online.libang.glyph`; main API is `GlyphAPI` / `Glyph`. Plugins linking the BetterHud Java API must recompile and depend on Glyph. Binary compatibility is not promised.
- Java 25 and Paper 26.x are required. Spigot, Fabric and 1.21.x adapters are removed initially.
- Regenerate/reapply the pack. The 26.3 shader templates live in `Glyph/shaders/26.3`; old custom shaders are not automatically converted to ShaderC/OIT. Older 26.x templates remain under `Glyph/shaders`.
- Different files at the same pack path now fail with both origins. Identical files coalesce. Resolve fonts/shaders/post-effects explicitly.
- `enable-protection: true` is rejected: invalid ZIP metadata conflicts with deterministic output. Disable it before reload.
- ZIP/folder outputs repair missing/changed files. `clear-build-folder: true` removes files absent from the compiled pack in the configured folder. Use a dedicated generated-pack directory.
- Libraries are bundled. Old `.libraries` files are unused; classloader injection and JVM add-opens are unnecessary.
- Upstream update checks and upstream telemetry IDs are disabled.
- Native dependency caching is opt-in. Legacy placeholders keep polling; identical sources now agree within one frame. Providers relying on repeated calls in one frame must adapt.

CraftEngine remains an independent integration using its async cache event. CustomNameplates and TAB remain independent; use the live validation matrix in GLYPH_PERFORMANCE.md.
