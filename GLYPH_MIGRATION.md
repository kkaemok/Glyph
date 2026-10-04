# Migrating from BetterHud

Stop the server and retain your existing BetterHud files. Install Glyph in place of BetterHud; running both HUD interceptors together is unsupported. Copy HUD, layout, text, image, popup, compass, background and player-data configuration to `plugins/Glyph`.

- New default namespace: `glyph`; an existing `namespace: betterhud` remains valid. New output defaults to `Glyph/build`. Review the old `build-folder-location`.
- Plugin ID is Glyph / `glyph`. Permissions use `glyph.*`; update grants. Commands retain their HUD semantics.
- The inherited public API retains `kr.toxicity.hud.api` on Paper and Velocity. `BetterHudAPI.inst()` and `BetterHud.getInstance()` return the same live core as `GlyphAPI.inst()`. Managers, players, popups, callbacks, value types, and Bukkit events use the original contracts directly. Paper declares `provides: [BetterHud]` for plugin dependency resolution.
- Glyph's implementation, entrypoints, typed state, and post-effects remain under `online.libang.glyph`. Early integrations compiled against the initial snapshot's renamed `online.libang.glyph.api.player`, manager, popup, or event types must recompile with the restored `kr.toxicity.hud.api` contracts; Glyph has no stable published API release yet.
- Java 25 and Paper 26.x are required. Spigot, Fabric and 1.21.x adapters are removed initially.
- Regenerate/reapply the pack. The 26.3 shader templates live in `Glyph/shaders/26.3`; old custom shaders are not automatically converted to ShaderC/OIT. Older 26.x templates remain under `Glyph/shaders`.
- Different files at the same pack path now fail with both origins. Identical files coalesce. Resolve fonts/shaders/post-effects explicitly.
- `enable-protection: true` is rejected: invalid ZIP metadata conflicts with deterministic output. Disable it before reload.
- ZIP/folder outputs repair missing/changed files. `clear-build-folder: true` removes files absent from the compiled pack in the configured folder. Use a dedicated generated-pack directory.
- Private libraries are bundled. Gson uses the platform's shared classes; bundled BetterCommand retains its original names. Both appear in upstream public method descriptors. Old `.libraries` files are unused; classloader injection and JVM add-opens are unnecessary.
- Upstream update checks and upstream telemetry IDs are disabled.
- Native dependency caching is opt-in. Legacy placeholders keep polling; identical sources now agree within one frame. Providers relying on repeated calls in one frame must adapt.

CraftEngine remains an independent integration using its async cache event. CustomNameplates and TAB remain independent; use the live validation matrix in GLYPH_PERFORMANCE.md.

Compatibility is verified against the complete neutral and platform API at
`UPSTREAM_BASE`, including nested classes and public member descriptors. It does
not cover BetterHud implementation internals, the removed Fabric API, or every
historical/future BetterHud release. Plugins that explicitly require a particular
BetterHud implementation/version or inspect the plugin's literal name may need
their own update. Velocity retains plugin ID `glyph`; plugins declaring a hard
Velocity dependency on ID `betterhud` must update that dependency. The original
Java API classes remain available on Velocity.

Glyph resource requests use a stable pack ID and retain unrelated packs. On the
first migration, remove the old BetterHud pack or reconnect/reapply the server's
complete pack set. Shared core shaders and `end_of_frame` need one explicit owner.
CraftEngine's integration rejects conflicting effective resources rather than
silently overriding its declared assets. See GLYPH_CONFIGURATION.md for the new
flow, directory-image and nine-slice syntax.
