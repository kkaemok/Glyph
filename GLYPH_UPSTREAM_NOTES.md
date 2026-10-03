# Glyph upstream review

Reviewed 2026-10-03. Base: BetterHud `dev`, commit `e6e835ec`.
Repository: https://github.com/toxicity188/BetterHud. Full history retained;
`upstream` is read-only by convention. Work branch: `codex/glyph`.

## Findings before implementation

`dev` differs from `master` in 15 files (35 additions, 34 deletions), primarily
dependency/version updates. It already uses Java 25, Kotlin 2.4.20, Gradle 9.8.0
and paperweight 2.0.0-beta.23. Build properties mention 26.3, but NMS modules stop
at 26.2 and bootstrap dispatch does not implement 26.3. Support metadata alone
is not evidence of runtime compatibility.

The repository has a Java public API, Kotlin shared implementation (`dist`),
Paper/Bukkit and Velocity bootstraps, nine NMS modules, two scheduler adapters,
Bedrock adapters, and a Fabric bootstrap/mod API. Fabric includes Polymer,
mixins and its own schedulers/compatibilities; remove it from the initial fork
without entangling future platform implementations with the Paper transport.

Rendering uses per-player tasks, tick-cached suppliers, mutable WidthComponent
builders, compiled font/image providers, conditions and popup iterators. Every
update rebuilds/compacts/sends the final component. Placeholder resolution
creates separate functions for identical sources, so a random or changing
provider can disagree across conditions within one frame (#266).

Paper and Velocity preserve external bossbars by remapping their packet state
onto dummy bars and the HUD bar. This is important existing behavior; keep it.
Paper NMS serializes Adventure/NMS components through JSON and discovers the
private bossbar operation enum reflectively. Inspect accessible Paper bridges
before replacing either. Netty buffer ownership and swallowed write promises
need explicit review during transport refactoring.

The head cache already loads asynchronously, expires after access and prevents
duplicate loads. Keep its behavior; shade its dependencies instead of using
URLClassLoader/Unsafe injection. Database providers support YAML and MySQL via
Hikari. Optional integrations are compileOnly. CraftEngine 26.9.1 integration
uses AsyncResourcePackCacheEvent, skips the initial reload, and attaches Glyph's
generated output. There is no dedicated CustomNameplates integration.

Resource generation shares a sorted task map, but silently ignores duplicate
paths. ZIP/folder output shares a hash stamp, commits it before output succeeds,
and hashes only bytes, not paths. Folder path cleanup uses string slicing.
These explain #480/#326; use one validated compilation and distinct output
cache identity. Post-effect and core shader collisions must fail explicitly.

## Required PRs/issues read

| Item | Finding / downstream decision |
| --- | --- |
| [PR 475](https://github.com/toxicity188/BetterHud/pull/475) | Open; cache immutable output/color before compact/conversion/send. Preserve force-update and invalidate on reload. Reported measurements belong to its author, not Glyph. |
| [PR 502](https://github.com/toxicity188/BetterHud/pull/502), [501](https://github.com/toxicity188/BetterHud/issues/501) | Open; legacy entity names reach MiniMessage. Prefer Paper component names, serialize safely once through the compatibility path. |
| [PR 447](https://github.com/toxicity188/BetterHud/pull/447) | Open; shades ExpiringMap only. Glyph will remove the entire runtime injector and bundle private dependencies. |
| [PR 500](https://github.com/toxicity188/BetterHud/pull/500) | Open; conditional fog/lightmap inputs fix Vulkan GUI variants; retain relevant modern changes. Does not prove 26.3 shader support. |
| [PR 355](https://github.com/toxicity188/BetterHud/pull/355) | Open; horizontal flow is useful, vertical support is only reserved. Extend absolute semantics incrementally. |
| [479](https://github.com/toxicity188/BetterHud/issues/479) | Open; do not simply remove vanilla world-text lightmapping globally. Match each supported vanilla shader and guard Glyph-specific changes. |
| [442](https://github.com/toxicity188/BetterHud/issues/442) | Open; compile the bounded asset candidates into fonts, select at runtime with fallback; arbitrary uncompiled textures cannot be sent as glyphs. |
| [266](https://github.com/toxicity188/BetterHud/issues/266) | Reproducer uses random placeholder; cache source values by player/frame/event, not independently per condition. |
| [383](https://github.com/toxicity188/BetterHud/issues/383) | Nine-slice requires preserving corners and tiling/stretching edges/center. |

Newer open items reviewed include #480 (ZIP/folder cache), #481 (RenderScale
comparator not antisymmetric), #482 (armor double counting), #483 (Vulkan),
#453 (Adventure 5), and dependency PRs #508/#516/#517. Dependency bumps should
be checked against server-provided APIs rather than adopted indiscriminately.

## Sync policy

Fetch `upstream dev`; review `git log` and `git diff` from the recorded base.
Port relevant Paper/Velocity, renderer, resource-pack, CraftEngine and shader
fixes as focused commits with source PR/commit attribution. Skip Fabric-only,
legacy 1.21 and Spigot fallbacks unless a concrete supported use needs them.
Never blindly merge `upstream/dev`. Update this file after each reviewed sync.

## Implementation order

1. Establish identity, attribution, build and migration documentation.
2. Paper/Velocity only, modern packages, shaded dependencies, Paper scheduler.
3. Render deduplication, safe names, frame consistency, tested pack output.
4. Isolate transport and version adapters; build against actual 26.x bundles.
5. Shader backends and post-effects; validate actual client assets/APIs.
6. Native typed state/segment invalidation, dynamic assets and layout extensions.
7. Ecosystem and reproducible profiling/integration checks.

Build and validation results are recorded in GLYPH_PERFORMANCE.md. Outstanding
support must remain explicit; never claim untested client/server versions.
