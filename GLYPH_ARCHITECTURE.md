# Glyph architecture

Glyph is an incremental downstream fork of BetterHud dev. Its renderer,
configuration loaders, popup/compass behavior, animation semantics and external
bossbar interception remain the foundation. MIT attribution stays intact.

## Boundaries

- `glyph-api`: platform-neutral public contracts, typed state and dependencies.
- `glyph-core`: configuration compilation, render snapshots, placeholder frame
  sampling, dependency-aware segments, fonts/images, packs and persistence.
- `glyph-paper`: authoritative gameplay HUD and modern Paper integration.
- `glyph-paper-api`: Paper-facing integration contracts; no NMS in public API.
- `glyph-velocity`: network/proxy transport and existing transition behavior.
- `glyph-velocity-api`: proxy integration contracts.
- `nms`: version adapters built with paperweight-userdev and Mojang names.
- `scheduler:paper`: global/async/location scheduling, with entity operations
  scheduled on the owning player's EntityScheduler in Paper code.
- `bedrock`: optional existing Geyser/Floodgate adapters.

Fabric is deferred. A future `glyph-fabric` can implement the neutral contracts
without introducing Fabric dependencies into Paper or making the Paper adapter
use a lowest-common-denominator scheduler.

## Rendering ownership

Legacy placeholders remain polling-compatible. Each render pass samples each
source once for the same player/event context. Native typed values use immutable
snapshots and dependency revisions. Explicit dependency declarations let native
HUD definitions reuse rendered output until an input changes. Animated or
legacy polling definitions continue ticking. Never infer that a arbitrary
third-party placeholder is static.

Final immutable component/color equality is checked before compacting and
transport conversion. Cache state is committed only after a successful send,
and invalidated on reload/removal. Force-update remains an escape hatch.
External bossbar changes still drive additionalComponent updates.

Keep per-player ticking initially; batch only after profiling demonstrates a
benefit and entity ownership remains correct. Stagger autosaves by UUID to
avoid synchronized disk/database bursts.

## Packs and shaders

Compile a sorted, validated map once for ZIP/directory outputs. Paths and bytes
both contribute to the hash. Identical collisions can coalesce; conflicting
font/shader/post-effect assets require an actionable diagnostic. Output caches
must verify the actual output and only commit after a successful write.

Dynamic assets must be precompiled into fonts. Bounded selectors prevent an
untrusted or changing identifier from causing runtime pack generation. Names
and fallbacks are resolved against the compiled candidate set.

Version-specific shader transformations belong to a backend. Compare against
the matching vanilla shader; preserve ordinary text rendering. Core shader
coexistence and `minecraft:end_of_frame` cannot be composed blindly. Post-effects
use the public Paper API on 26.3; unsupported versions must report capability
absence explicitly. OpenGL/Vulkan validation needs real clients.

## Integrations and public API

CraftEngine's cache event remains the pack handoff boundary. Optional APIs stay
compileOnly. CustomNameplates and TAB stay independent plugins; pack diagnostics
and scoped glyph/shader behavior support coexistence. External gameplay plugins
own their state and integrations; Glyph contains no RPG-specific behavior.

All project packages migrate to `online.libang.glyph`. This deliberately breaks
BetterHud binary plugin linkage; retain configuration syntax and document the
required recompile. No NMS types enter the public typed-state API.
