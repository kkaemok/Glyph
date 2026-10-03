# Glyph validation and performance

No Glyph production performance claim has been established. Upstream PR #475
reports allocation reductions; those numbers are not a Glyph benchmark.

## Build baseline

Unmodified upstream `e6e835ec`: invoked `gradlew.bat build --console=plain` with
installed Java 25 on Windows. Results will be recorded after completion.

## Reproducible profiling

Use the same Java/server/client versions, configs, hardware and warm-up for
BetterHud and Glyph. Save spark allocation/CPU profiles or JFR recordings.
Run each scenario repeatedly for 1, 20 and 100+ players where feasible:

| Scenario | Capture |
| --- | --- |
| Static HUD | component rebuilds, compact/conversion calls, packets, allocations |
| Changing native values | invalidated segments, unrelated segment reuse |
| Many legacy placeholders | evaluations per frame, CPU, sampled consistency |
| Animations/popups | frame correctness, allocations, lifecycle leaks |
| External bossbars | name/style/progress ordering, packets, buffer ownership |
| CraftEngine pack | generation time, hashes, collisions, reload count |
| Autosave | peak tick/IO time, staggering, DB contention |

Do not centralize tick tasks until both strategies are measured under equivalent
load. Server simulation cannot verify client shader correctness.

## Manual integration matrix

On each supported Paper 26.1.x/26.2/26.3 and Folia equivalent, verify join,
quit, reconnect, reload, HUD toggle, popup expiry, compass, force-update,
dimension change and teleport while tasks run. Check vanilla Wither/Dragon bars,
TAB bars, multiple plugin bars, changing names/colors/progress and bar removal.
Test Velocity switches in both directions with backend HUD enabled.

OpenGL and Vulkan clients: verify vanilla text, world labels, inventory, TAB
bitmap logos, CraftEngine textures and CustomNameplates fonts alongside Glyph.
Check shader compilation logs. Test post-effect add/remove/set/clear on 26.3,
missing effect assets and conflicting `minecraft:end_of_frame` definitions.

CraftEngine: regenerate twice, compare pack hashes/assets, ensure no reload
loop, preserve external assets, test both ZIP and folder, switch output types,
delete an output file, rename an asset and reproduce collision diagnostics.

## Automated checks

Tests cover output equality/invalidation, frame sampling, typed-state and
dependency invalidation, layouts/geometry, pack paths/conflicts/hashing/output
recovery, safe MiniMessage names and shader-scale ordering. Record actual test
counts/build outcomes below; live checks remain unverified until performed.
