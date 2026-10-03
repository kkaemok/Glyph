# Glyph validation and performance

No Glyph production performance claim has been established. Upstream PR #475
reports allocation reductions; those numbers are not a Glyph benchmark.

## Build baseline

Unmodified upstream `e6e835ec`: attempted `gradlew.bat build --console=plain`
with installed Java 25 on Windows, including a separate detached validation
checkout. Its 1.21 R3/R4 paperweight setup failed; remaining legacy/Fabric setup
was stopped after those failures to release resources. A clean full upstream
baseline was **not established**. Logs are retained under `.validation` locally.

Glyph `gradlew.bat build --console=plain --max-workers=2`: passed on Java 25,
including both distributions, 34 core tests and the existing Paper and Velocity
compatibility checks (36 tests total, zero failures/errors/skips). The
26.1.2 adapter/transport and 26.3 modern adapter compile. The modern adapter also
compiled separately against `26.2.build.+` using quoted Gradle property
`'-PpaperModernVersion=26.2.build.+'`. This proves compilation, not live binary
compatibility for every patch release.

The final build completed in 5m 34s. Both distribution archives passed ZIP
integrity checks. Their bootstraps target Java 25, metadata identifies Glyph,
private libraries are relocated, MySQL's JDBC service is included, 26.3 templates
are bundled, and `META-INF/LICENSE-Glyph.txt` exactly matches the preserved MIT
license. No Fabric, 1.21 adapter or old BetterHud implementation classes remain
in either distribution. Logs and the local archive checker are under `.validation`.

## Pack-format merge regression (2026-10-04)

Client OpenGL/Vulkan logs and the downloaded CraftEngine pack confirmed Glyph
fonts/textures were present, but the `glyph_26_3` overlay had `max_format: [97,0]`
while the client used format 97.1. Minecraft's own 26.3 overlay codec selected no
Glyph overlay for that cached pack. This isolates a real metadata failure without
requiring a shader syntax error in the client log.

Glyph now emits explicit full upper bounds, including all minor versions of the
supported major, to survive CraftEngine's integer-to-array normalization. Parsing
also preserves Minecraft's different integer lower/upper semantics, modern
overlays without legacy `formats`, and component descriptions. Conflict checks
use both resource-format components; modern generated metadata omits obsolete
format fields. Root range intersections reject incompatible pack combinations.

The full build passed in 2m 34s: 43 core tests and Paper/Velocity compatibility
checks, 45 total with zero failures/errors/skips. Both jar archives passed checks.
`tools/packs/ValidatePackMetadata.java`, using the official client and libraries,
confirmed generated metadata activates exactly the expected overlay at 84.0,
88.0 and 97.1, excludes 98.0, and activates `glyph_26_3` after a round-trip through
CraftEngine 26.9.1's actual `Overlay` parser/serializer. The cached pack selected
`[]`; corrected metadata selected `[glyph_26_3]`. Regenerate the distributed pack
after installing the rebuilt jar. These codec checks do not certify visual
rendering on the user's client; a refreshed pack/client retest remains necessary.

Minecraft's [minor-format rules](https://www.minecraft.net/en-us/article/minecraft-snapshot-25w31a)
define integer upper bounds as every minor of that major;
[26.3 uses resource format 97.1](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3).

## Reproducible profiling

### Player initialization regression (2026-10-04)

A reported Leaf 26.3 join failure exposed a base-constructor call into
`HudPlayerBukkit.scheduleOwned` before Kotlin assigned its `player` field. Two
regression tests reproduced the same null-owner stack through the real
`HudPlayerImpl` constructor before the fix. Task holders now defer creation;
injection starts tasks after platform fields, default objects and transport are
ready. Reload also starts tasks after rebuilding objects. A third test checks
that a failed scheduler restart cancels the previous task without leaking it.

`gradlew.bat :glyph-core:test build --console=plain --max-workers=2` passed in
1m 54s: 37 core tests plus Paper/Velocity compatibility checks, 39 total with
zero failures/errors/skips. Both distributions were rebuilt. The regression is
verified without a server; no live Leaf/client retest was performed here.

### Server profiling scenarios

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

## Recorded primitive profile (2026-10-03)

Command: `gradlew.bat :glyph-core:profileCore`. Windows, Intel Core i5-1340P,
Java 25.0.3, JFR profile settings. Each case warms up for 20,000 iterations and
measures 100,000 frames. Render lambdas are compiled once, matching segment
ownership. Allocation counters use the JVM ThreadMXBean. The resulting recording
is `glyph-core/build/core-profile.jfr`; its recorded run includes 448 execution
samples and 1,548 allocation samples over six seconds.

These are state/cache instances, **not connected Minecraft players**. Values
below measure actual `HudState`, `DependencyCache`, Adventure component creation
and `HudRenderCache` send decisions. They exclude Paper scheduling, real layout
rendering, plugins, network sends, JSON conversion and client work. One run under
shared build-machine load is a reproducible smoke profile, not a production or
BetterHud comparison. Timings varied between runs.

| Instances | Scenario | ns / instance frame | bytes / instance frame | Rebuilds | Send decisions |
| --- | --- | ---: | ---: | ---: | ---: |
| 1 | static | 75 | 0 | 0 | 0 |
| 1 | changing health | 852 | 995 | 100,000 | 100,000 |
| 20 | static | 25 | 0 | 0 | 0 |
| 20 | changing health | 356 | 821 | 2,000,000 | 2,000,000 |
| 100 | static | 23 | 0 | 0 | 0 |
| 100 | changing health | 324 | 821 | 10,000,000 | 10,000,000 |

The static path's reuse and zero steady-state allocation are established only
for this primitive harness. No server TPS or whole-renderer speed claim follows.
We retained per-player ticking and the existing JSON component bridge because
there is no equivalent live benchmark justifying a scheduling/bridge replacement.

## Offline shader checks

Used the official release client jars and their actual includes: resource format
84 for 26.1.2, 88 for 26.2, 97.1 for 26.3. LWJGL ShaderC 3.4.3 came from the 26.3
client manifest. `tools/shaders/prepare_checks.py` expands Minecraft imports and
Glyph compiler hooks with representative constants/layout IDs. The Java driver
compiles SPIR-V for OpenGL 4.5 and Vulkan 1.1, forces GLSL 450, and enables automatic
uniform binding/location mapping. These are explicit offline test settings;
they do not claim to reproduce every client compiler option.

- 26.1.2: 32 checks passed.
- 26.2: 32 checks passed.
- 26.3: 56 checks passed, including GUI, world, see-through, grayscale,
  experience-text hiding, OIT accumulation, depth and transmittance variants.

All 120 checks pass. Ordinary world-text lightmapping is preserved. Actual
shader linking, draw-time uniforms, visual isolation, driver behavior and GPU
correctness require OpenGL/Vulkan Minecraft clients. Offline shader success is
not a visual compatibility certificate.

## Validation still required

No live Paper/Folia server with clients, Velocity network, CraftEngine,
CustomNameplates or TAB was available in this task. The matrix above remains
unexecuted. Production packet/allocation measurements at 1/20/100+ players,
conversion benchmarks, reconnect/disable restoration and server switches must be
performed before advertising production support/performance. CraftEngine's cache
event exposes declared/external resource roots; resources generated later by its
own pipeline need final-pack verification as well.
