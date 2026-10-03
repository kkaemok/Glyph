You are working on a Minecraft HUD plugin project named **Glyph**.

Your job is to take the latest `dev` branch of BetterHud and turn it into Glyph, a modern, high-performance, Paper-first fork that continues tracking BetterHud upstream.

Upstream repository:

https://github.com/toxicity188/BetterHud

Upstream branch:

`dev`

Do NOT base the project on `master`.

---

# 1. PRIMARY GOAL

Create **Glyph**, a high-performance BetterHud fork focused initially on:

- Paper
- Velocity
- Minecraft 26.x
- Java 25
- Kotlin
- modern Paper APIs
- direct NMS packets through paperweight-userdev where necessary
- resource-pack HUD rendering
- BetterHud configuration compatibility where practical

Glyph should preserve the useful capabilities of BetterHud while cleaning up old architecture, reducing unnecessary allocations/work, improving packet efficiency, improving resource-pack/shader handling, and adding new 26.x-specific features.

This is NOT:

- a rewrite from scratch
- a cosmetic rename
- a Tunacan-specific plugin
- a project that blindly merges every upstream BetterHud commit

It should be a maintainable downstream fork similar in philosophy to how Leaf tracks Paper.

---

# 2. FIRST STEP: INSPECT BEFORE MODIFYING

Before changing code:

1. Fetch the latest BetterHud `dev` branch.
2. Inspect:
   - repository structure
   - build scripts
   - NMS modules
   - Paper/bootstrap code
   - Velocity implementation
   - Fabric implementation
   - renderer
   - HUD update loop
   - placeholders
   - layouts
   - shaders
   - resource-pack generator
   - CraftEngine integration
   - CustomNameplates-related compatibility if any
   - player head cache
   - database system
   - packet interception
   - scheduler abstraction
   - open PRs
   - relevant open issues
3. Compare `dev` with `master`.
4. Do not assume details from this prompt override newer upstream code. If BetterHud has already fixed or redesigned something, adapt the Glyph plan to the newer implementation rather than reverting it.

Keep a document such as:

`GLYPH_UPSTREAM_NOTES.md`

containing important upstream behavior and which Glyph patches intentionally diverge.

---

# 3. PROJECT IDENTITY

Rename the project to:

`Glyph`

Suggested description:

`A high-performance HUD engine for Paper and Velocity, based on BetterHud.`

Suggested base package:

`online.libang.glyph`

If changing every package immediately would cause a huge risky diff, package migration may be staged, but the final target should not remain under `kr.toxicity.hud`.

Preserve all required MIT attribution and license notices from BetterHud.

Do not hide BetterHud's origin.

---

# 4. VERSION POLICY

For the initial Glyph architecture:

Primary support:

- Minecraft 26.1.x
- Minecraft 26.2
- Minecraft 26.3
- prepare cleanly for 26.4
- later 27.x

Use:

- Java 25
- modern Kotlin
- current compatible Gradle
- current compatible paperweight-userdev
- modern Paper API

Do NOT prioritize Minecraft 1.21.x.

Do not allow old-version support to contaminate the new core architecture.

Older support can potentially return later through isolated adapters if there is a compelling reason.

26.x is the baseline, but do not hardcode the entire core around the literal number `26`.

Version-specific behavior must remain isolated.

---

# 5. UPSTREAM STRATEGY

BetterHud `dev` remains Glyph's upstream.

Create a clean strategy for future syncing.

Categorize upstream changes:

PORT OR REVIEW:
- performance improvements
- Paper fixes
- Velocity fixes
- resource-pack fixes
- CraftEngine fixes
- renderer fixes
- useful layout/HUD features
- shader fixes relevant to supported versions
- bug fixes relevant to Glyph

IGNORE UNLESS NEEDED:
- Fabric-only changes during the initial Paper-first phase
- 1.21-only compatibility fixes
- Spigot/Bukkit fallback code Glyph does not need
- obsolete compatibility work

Do not blindly `git merge upstream/dev` if it would destroy Glyph architecture.

Prefer understandable downstream patches.

Document major deviations.

---

# 6. INITIAL PLATFORM POLICY

Initial platforms:

- Paper
- Velocity

Fabric should be reintroduced later.

IMPORTANT:

Remove BetterHud's current Fabric implementation initially, but do NOT design Glyph in a way that prevents Fabric from being added later.

Remove initially:

- Fabric bootstrap
- mod-api
- Fabric Loader dependencies
- Fabric API dependencies
- Fabric publishing
- Fabric repositories/build tooling where no longer necessary
- Polymer/mod-only compatibility layers
- abstractions that exist only because BetterHud currently targets Fabric

But create clean platform boundaries so this can exist later:

```text
glyph-core
glyph-api
glyph-paper
glyph-velocity
glyph-fabric   # future
```

Fabric should eventually implement the same core Glyph concepts without forcing Paper through a lowest-common-denominator API.

---

# 7. DO NOT ADD TUNACAN-SPECIFIC CODE

Glyph must remain generic.

Tunacan or any RPG plugin should consume Glyph through its API.

Do NOT add:

- Tunacan classes
- Tunacan hooks
- RPG-specific state
- Typewriter-specific behavior
- custom skill logic
- game-specific HUD assumptions

The public API should make those integrations possible externally.

---

# 8. PAPER-FIRST ARCHITECTURE

Glyph is Paper-first.

Do not retain generic Bukkit/Spigot compatibility layers simply because BetterHud had them.

Use modern Paper APIs where they simplify the design.

Examples:

- Paper scheduling APIs
- EntityScheduler
- AsyncScheduler
- GlobalRegionScheduler
- Adventure APIs
- modern resource-pack APIs
- Paper connection APIs
- Paper 26.3 post-effect API
- paper-plugin.yml if suitable
- paperweight-userdev for NMS

Avoid runtime checks like:

`if Paper then ... else Bukkit ...`

when Glyph only supports Paper.

---

# 9. SCHEDULER CLEANUP

BetterHud currently has generic Bukkit and Paper/Folia scheduler abstractions.

Simplify them.

Use Paper scheduler APIs as the baseline.

For player/entity-owned operations, prefer:

`player.scheduler`

or the appropriate Paper EntityScheduler instead of scheduling based on a player's current location.

Do not use a RegionScheduler for an entity operation when EntityScheduler is the correct primitive.

Preserve Folia correctness where possible.

---

# 10. PACKET/NMS STRATEGY

BetterHud already uses direct NMS packets and Netty interception for its bossbar HUD system.

KEEP the useful technique.

DO NOT replace it with PacketEvents or ProtocolLib by default.

Use:

- paperweight-userdev
- Mojang-named 26.x internals
- small version-specific packet adapters

Use public Paper/Adventure APIs first.

Only use NMS where the supported API cannot provide the required behavior.

Desired hierarchy:

1. Paper API
2. Adventure API
3. Paper connection/resource APIs
4. NMS via paperweight-userdev

Do not use raw NMS everywhere merely because it is available.

---

# 11. PACKET TRANSPORT ABSTRACTION

Create or clean up an internal abstraction similar to:

```kotlin
interface PacketTransport {
    fun inject(player: GlyphPlayer)
    fun updateHud(player: GlyphPlayer, state: HudRenderState)
    fun removeHud(player: GlyphPlayer)
}
```

Possible modules:

```text
transport-api
transport-paper-26.1
transport-paper-26.2
transport-paper-26.3
```

Only split versions when actual internals differ.

If 26.2 and 26.3 can safely share implementation, share them.

Do not duplicate entire files just because the Minecraft version changed.

---

# 12. PRESERVE BETTERHUD BOSSBAR INTERCEPTION

BetterHud currently:

- creates its own HUD bossbar
- inserts dummy bossbars
- injects a `ChannelDuplexHandler`
- intercepts outgoing `ClientboundBossEventPacket`
- tracks external bossbars
- rewrites/remaps them
- keeps real plugin/vanilla bossbars compatible with the HUD
- can incorporate an external bossbar's text into its rendered HUD

Preserve this functionality unless you find a better equivalent.

However, refactor the responsibilities.

Do NOT leave one giant class handling:

- HUD rendering
- packet creation
- Netty interception
- external bossbar state
- width calculations
- component conversion
- dummy bar management

Prefer something conceptually like:

```text
BossBarHudTransport
BossBarInterceptor
BossBarStateTracker
BossBarLayoutManager
BossBarPacketCodec
```

Do not over-engineer it into dozens of trivial interfaces. Split only where responsibility boundaries are real.

---

# 13. REMOVE UNNECESSARY REFLECTION

BetterHud currently uses reflection/adapted getters in NMS code, including internal bossbar operation discovery.

Because Glyph targets 26.x and uses paperweight with unobfuscated Mojang names:

- inspect whether reflection can be replaced by direct named APIs/classes
- remove reflection where direct access is reasonably stable within version modules
- keep reflection only where it genuinely reduces version duplication or is required

Do not blindly remove working reflection if the direct replacement makes compatibility worse.

---

# 14. MAJOR PERFORMANCE TARGET

The central Glyph principle:

**If the player's visible HUD would remain identical, Glyph should do as close to zero work as possible.**

BetterHud currently performs too much polling/rebuilding.

Move toward:

```text
state changes
→ dependency tracking
→ dirty elements
→ dirty layouts
→ render affected segments
→ compare final snapshot
→ send only changed packets
```

Do not rebuild the full HUD just because a timer fired.

---

# 15. PORT / REIMPLEMENT PR #475

Investigate BetterHud PR #475:

`perf: skip unchanged HUD boss bar updates`

This is a mandatory optimization concept.

The reported benchmark showed dramatic reductions in allocations, including approximately:

- `HudPlayerImpl.update`: ~803 MB/s → ~264 MB/s
- NMS `showBossBar`: ~467 MB/s → ~5 MB/s
- `Component.compact`: ~57 MB/s → ~0.6 MB/s

Do not merely cherry-pick without reading it.

Integrate the behavior properly into Glyph's renderer.

At minimum:

- cache last rendered component/state
- cache relevant bossbar color/style
- skip `Component.compact()`
- skip NMS conversion
- skip packet creation
- skip packet sends

when output is unchanged.

Then improve beyond the PR with dirty-state rendering.

---

# 16. DIRTY-STATE / DEPENDENCY-AWARE RENDERER

Introduce explicit dependency tracking.

Example:

A health change should dirty:

- health number
- health bar
- low-health condition
- health-related animation if relevant

It should NOT recompute:

- skill icons
- quest text
- money
- class icon
- compass
- static decoration

unless those depend on health.

Possible model:

```text
HudState
├─ health
├─ mana
├─ location
├─ variables
├─ placeholders
└─ plugin-defined values

DependencyGraph
health
 ├─ health_text
 ├─ health_bar
 └─ low_health_condition
```

Do not force every placeholder to use this immediately if that makes migration impossible.

Design a hybrid system where old polling placeholders still work, while Glyph-native values can become event/dirty-driven.

---

# 17. CENTRALIZE TICKING WHERE IT HELPS

BetterHud currently creates repeating tasks per player for several operations.

Investigate whether Glyph should centralize:

- HUD ticks
- autosaves
- location updates
- periodic placeholder work

Avoid hundreds of independent scheduler tasks if a batched ticker performs better.

Do not centralize blindly.

Benchmark both approaches.

Stagger expensive periodic work, especially autosaves, so all players do not save in the same tick.

---

# 18. REMOVE TRIVIAL HOT-PATH ALLOCATIONS

Inspect hot loops.

For example, BetterHud currently does patterns like:

```kotlin
updateTask.filter { ... }
```

for repeated player updates.

Avoid short-lived collections when a normal loop is sufficient.

Also inspect:

- repeated `ArrayList` creation
- component concatenation
- string building
- placeholder result lists
- conversion wrappers
- repeated maps
- codepoint arrays
- unnecessary copies of Netty buffers
- repeated MiniMessage parsing
- repeated JSON serialization

Use profiling rather than micro-optimizing everything blindly.

---

# 19. COMPONENT RENDERING

BetterHud currently builds multiple `WidthComponent`s, concatenates them, calls `finalizeFont()`, then builds/compacts the final component.

Improve this.

Compile HUD definitions into reusable segments.

Conceptually:

```text
CompiledHud
├─ StaticSegment
├─ DynamicSegment(health)
├─ StaticSegment
├─ DynamicSegment(cooldown)
└─ StaticSegment
```

When one dynamic segment changes, do not regenerate unrelated static data.

Cache:

- static component trees
- glyph measurements
- font metadata
- parsed static MiniMessage
- static layout geometry

where safe.

---

# 20. ADVENTURE / NMS COMPONENT CONVERSION

Inspect current BetterHud conversions.

BetterHud currently has paths equivalent to:

```text
NMS Component
→ JSON
→ Adventure Component
```

and reverse.

This can be expensive.

Search Paper 26.x APIs and CraftBukkit internals for a cleaner direct conversion path.

Benchmark before changing.

Do not create a risky custom serializer merely to avoid JSON.

For external bossbars, conversion frequency may already be low enough.

Optimize the main HUD rendering path first.

---

# 21. PLACEHOLDER SYSTEM

Retain compatibility with BetterHud placeholders where practical.

However, improve the internal value model.

BetterHud already distinguishes number/string/boolean containers to some extent.

Push the system toward strongly typed values, conceptually:

```kotlin
sealed interface HudValue {
    data class Number(val value: Double) : HudValue
    data class Boolean(val value: kotlin.Boolean) : HudValue
    data class Text(val value: String) : HudValue
    data class Component(val value: net.kyori.adventure.text.Component) : HudValue
    data class Identifier(val value: Key) : HudValue
}
```

Exact implementation can differ.

Goals:

- less reparsing
- less string conversion
- safer MiniMessage behavior
- dynamic identifiers/assets
- easier dependency tracking

Do not unnecessarily break existing placeholder configs.

---

# 22. PR #502 / ISSUE #501

Inspect and port/reimplement BetterHud PR #502:

`fix(bukkit): make entity name placeholders MiniMessage-safe`

Root cause:

legacy `§` formatting can reach MiniMessage parsing and repeatedly throw parsing exceptions.

Fix the root cause in Glyph.

Prefer typed Adventure components where possible rather than repeatedly serializing names through strings.

Add tests.

---

# 23. ISSUE #266: CONDITIONAL FLICKER

Investigate BetterHud issue #266 involving conditional elements occasionally flickering or behaving as though no condition matched.

Possible causes include inconsistent placeholder values during one frame.

Glyph should render against a consistent per-frame/player state snapshot where practical.

One render pass should not see mutually inconsistent values merely because different placeholders were evaluated at different moments.

Add regression tests if reproducible.

---

# 24. DYNAMIC IMAGES / ISSUE #442

BetterHud currently makes dynamic image selection cumbersome because images are commonly resolved statically during config loading.

Implement dynamic asset selection.

Desired config concepts could include:

```yaml
image:
  source: "skills/[skill_id]"
```

or another clean syntax.

Support:

- dynamic image identifier
- cached lookup
- fallback image
- missing-resource diagnostics
- dependency-aware invalidation

Do not require hundreds of duplicate image declarations for dynamic skill/icon systems.

Preserve existing image syntax.

---

# 25. MODERN LAYOUT ENGINE

Preserve BetterHud's current absolute layout semantics for compatibility.

Add modern layout primitives:

- absolute
- row
- column
- stack

Potential properties:

- gap
- padding
- anchor
- alignment
- justify
- dynamic width/height
- min/max sizing
- child ordering

Inspect BetterHud PR #355:

`flow layout support`

Use it as reference.

Do not necessarily cherry-pick it verbatim if a proper row/column abstraction is cleaner.

Keep runtime layout calculations efficient.

Precompute static geometry.

---

# 26. NINE-SLICE BACKGROUNDS

Inspect BetterHud issue #383.

Implement scalable nine-slice / nine-patch backgrounds suitable for:

- panels
- dialogue boxes
- quest HUDs
- skill containers
- tooltips
- RPG UI

Preserve corners while allowing edges and center to scale/tile appropriately.

Avoid forcing creators to manually compose nine separate images for every size.

---

# 27. RESOURCE PACK COMPILER

Refactor resource generation around one deterministic compiler.

Conceptually:

```kotlin
interface PackOutput {
    fun write(pack: CompiledPack)
}
```

Outputs:

- ZIP
- directory
- hosted output if applicable

Avoid maintaining completely separate logic for ZIP and folder generation.

Address BetterHud reports where ZIP generation works while folder output behaves incorrectly.

Add:

- deterministic ordering
- conflict detection
- namespace validation
- duplicate asset detection
- helpful diagnostics
- pack hashing
- cached outputs where appropriate

---

# 28. MULTI-PACK / MODERN RESOURCE PACK API

Minecraft/Paper 26.x supports modern Adventure resource-pack requests.

Do not assume the world forever requires one giant merged ZIP.

Investigate when Glyph can use resource-pack stacking.

Possible strategy:

```text
Independent packs
→ send as multiple ResourcePackRequest entries

Conflicting/shared shader/font resources
→ merge through Glyph pack compiler
```

Do not split packs if the client would produce incorrect ordering/conflicts.

Resource-pack correctness is more important than ideological purity.

---

# 29. CRAFTENGINE INTEGRATION

BetterHud already integrates with CraftEngine and uses `AsyncResourcePackCacheEvent`.

Retain and improve this.

Glyph should have first-class CraftEngine compatibility.

Goals:

- reliable pack merge/stack behavior
- no duplicate reload loops
- asynchronous generation
- conflict detection
- proper resource ordering
- preserve CraftEngine assets
- avoid unnecessary rebuilds

Track current CraftEngine API instead of relying on old BetterHud integration assumptions.

---

# 30. CUSTOMNAMEPLATES COMPATIBILITY

Do NOT merge CustomNameplates into Glyph.

Provide first-class compatibility.

Investigate:

- font namespaces
- glyph allocation
- offset glyphs
- background assets
- bitmap fonts
- shader interactions
- resource-pack collisions

Glyph should coexist with CustomNameplates without corrupting its fonts or rendering.

---

# 31. FIX GLOBAL SHADER INTERFERENCE / ISSUE #479

This is high priority.

BetterHud's global text shader modifications can affect unrelated text/custom bitmap fonts.

Investigate issue #479 carefully.

Glyph's shader behavior must isolate HUD-specific rendering as much as possible.

Normal Minecraft/custom plugin text outside Glyph should retain vanilla/expected behavior.

Test against:

- TAB
- CraftEngine
- CustomNameplates
- vanilla text
- other custom bitmap fonts

---

# 32. SHADER ARCHITECTURE

Create an explicit shader compatibility layer.

Do not scatter version-specific shader conditions everywhere.

Conceptually:

```text
ShaderBackend
├─ 26.1
├─ 26.2
├─ 26.3
└─ future 26.4 / 27.x
```

Share implementations when possible.

Minecraft 26.3 changes shader behavior significantly.

Account for current 26.3 rules, including:

- ShaderC-based compilation
- `#include`
- explicit input/output locations
- OIT-related changes
- removed/changed text-background shaders
- Vulkan compatibility
- OpenGL compatibility

Do not assume unsupported core-shader overrides will remain stable.

Keep them isolated so future Minecraft versions can replace the implementation.

---

# 33. PR #500

Inspect BetterHud PR #500.

Relevant part:

- 26.2 Vulkan support/fixes

Ignore irrelevant old-version portions where unnecessary.

Port or reimplement the useful Vulkan fixes.

Then validate behavior on:

- OpenGL
- Vulkan

for supported 26.x versions.

---

# 34. MINECRAFT 26.3 POST EFFECTS

Add proper Glyph support for Minecraft 26.3 post-processing effects.

Use Paper's public `PlayerPostEffects` API where available.

Do NOT execute `/posteffect` commands when Paper exposes a supported API.

Expose a Glyph API conceptually like:

```kotlin
glyph.postEffects(player).add(key)
glyph.postEffects(player).remove(key)
glyph.postEffects(player).clear()
glyph.postEffects(player).set(keys)
```

Potential uses:

- low-health vignette
- blur
- dungeon color grading
- damage distortion
- poison/magic effects
- cinematic effects
- environmental screen treatment

These are screen effects, not HUD widget replacements.

---

# 35. END-OF-FRAME POST EFFECT

Support resource-pack `minecraft:end_of_frame` carefully.

Because resource-pack priority can cause one definition to override another:

- detect conflicts
- document behavior
- avoid silently overwriting external packs
- integrate with the Glyph pack compiler

Do not pretend arbitrary post effects can always be composed automatically.

If composition is technically unsafe, emit a clear conflict diagnostic.

---

# 36. FUTURE 3D HUD SUPPORT

Minecraft 26.3 includes renderer work related to 3D HUD/depth integration.

Do NOT build core Glyph functionality around unsupported internals yet.

However, structure render backends so future official APIs can be added.

Conceptually:

```kotlin
interface HudRendererBackend
```

with current glyph/font rendering and future backends potentially coexisting.

Do not implement speculative NMS hacks unless they offer a concrete benefit today.

---

# 37. PLAYER HEAD CACHE

BetterHud already has a reasonable async head-loading/cache system.

Do not rewrite good code merely because it is old.

Retain:

- async head loading
- cache expiration
- duplicate-load prevention

But remove brittle runtime dependency loading.

Inspect whether ExpiringMap should:

- be shaded
- be replaced by a maintained cache
- be replaced with a small internal implementation

Base the decision on dependency cost and actual behavior.

---

# 38. PR #447 / DEPENDENCY LOADING

Investigate BetterHud PR #447.

BetterHud has had problems caused by runtime classloader injection, including modern Java/Paper classloader behavior.

Glyph should avoid homemade runtime `URLClassLoader` mutation and Unsafe tricks where practical.

Prefer:

- shaded private libraries
- Paper-supported dependency mechanisms
- compileOnly optional integration APIs
- normal dependency management

Do not keep fragile classloader hacks for legacy Bukkit compatibility Glyph no longer needs.

---

# 39. PAPER 26.x FEATURES

Actively inspect the Paper 26.1, 26.2, 26.3 APIs and release changes.

Use useful modern features when they simplify Glyph.

Already relevant:

## 26.1+
- unobfuscated Minecraft server internals
- cleaner paperweight userdev
- Java 25
- modern resource-pack APIs

## 26.2+
- modern Paper connection APIs
- Adventure 5 changes
- client connection/configuration APIs

## 26.3+
- PlayerPostEffects
- new shader/post-effect pipeline
- current renderer changes

Do not use new APIs merely because they exist.

Adopt only features that improve performance, architecture, correctness, or maintainability.

---

# 40. CONNECTION APIs

Investigate Paper's:

- PlayerGameConnection
- configuration connection APIs
- connection lifecycle events

Potential future uses:

- resource-pack lifecycle
- server switching
- configuration-stage preparation
- client metadata

Do NOT use connection reconfiguration casually.

Treat configuration-stage manipulation as experimental unless there is a clear need.

---

# 41. VELOCITY

Velocity remains first-class.

Its job is mainly:

- network coordination
- backend transitions
- player/server state handoff where appropriate
- possibly resource-pack/network metadata coordination

Do not move gameplay HUD calculation to Velocity unnecessarily.

Paper remains authoritative for normal HUD rendering/state.

Aim to reduce visual resets when switching servers if possible.

---

# 42. FABRIC LATER

After Paper/Velocity Glyph is stable, reintroduce Fabric.

Do not do this during initial modernization unless the Paper architecture is already stable.

Future structure should allow:

```text
Glyph Core
├─ Paper transport
└─ Fabric transport
```

Fabric should not force Glyph back into generic low-level abstractions everywhere.

Platform-specific integrations may remain platform-specific.

---

# 43. API DESIGN

Create a stable public Glyph API.

External plugins should be able to:

- show/hide HUDs
- query HUDs
- manipulate player HUD state
- trigger popups
- register placeholders/value providers
- trigger/remove post effects
- interact with layouts where appropriate
- register integrations/providers where useful

Avoid exposing NMS internals through the public API.

Avoid leaking BetterHud implementation details unnecessarily.

Maintain BetterHud API compatibility where inexpensive and useful, but Glyph's long-term API should be coherent.

---

# 44. CONFIG COMPATIBILITY

Existing BetterHud HUD configurations should continue working wherever practical.

New Glyph features should extend rather than arbitrarily replace existing syntax.

If a breaking config change is necessary:

- document it
- provide migration diagnostics
- consider automatic migration where safe

Do not make existing BetterHud users rebuild entire HUD packs just to use Glyph.

---

# 45. TESTING

Add tests for logic that does not require a live Minecraft server.

Important areas:

- placeholder parsing
- typed value conversion
- layout calculations
- nine-slice sizing
- dependency graph invalidation
- render state equality
- packet state/deduplication logic
- pack conflict detection
- MiniMessage escaping
- config migration

For server-specific behavior, create reproducible manual/integration test instructions.

---

# 46. PERFORMANCE TESTING

Use actual profiling.

Test scenarios:

- 1 player
- 20 players
- 100+ simulated/real players where feasible
- static HUD
- rapidly changing health/mana
- many placeholders
- animated HUD
- many popups
- many external bossbars
- CraftEngine merged pack

Measure:

- CPU
- allocations
- packet sends
- component rebuild count
- placeholder evaluations
- serialization calls
- cache hit/miss rate
- pack generation time

Use spark and/or JVM profiling where appropriate.

Do not claim something is faster without measurement.

---

# 47. IMPORTANT BETTERHUD ITEMS TO REVIEW

At minimum inspect these before implementation:

- PR #475
  - unchanged HUD bossbar update optimization

- PR #500
  - shader/Vulkan fixes

- PR #502
  - MiniMessage-safe entity names

- PR #447
  - ExpiringMap/runtime dependency loading issue

- PR #355
  - flow layout

- issue #479
  - global shader interference

- issue #442
  - dynamic images/layout limitations

- issue #266
  - conditional element flickering

- issue #383
  - scalable/nine-slice backgrounds

Also inspect newer PRs/issues added after this prompt was written.

Do not assume this list is complete.

---

# 48. CODE QUALITY

Prefer:

- immutable compiled configuration
- small focused classes
- clear hot-path ownership
- minimal allocations
- explicit platform boundaries
- direct modern APIs
- measured optimizations
- comments explaining strange packet/shader behavior
- tests for tricky logic

Avoid:

- abstraction for abstraction's sake
- giant god classes
- runtime reflection everywhere
- unnecessary string conversions
- unnecessary scheduler tasks
- global mutable state where avoidable
- raw version checks scattered through core
- blindly copying BetterHud legacy architecture

---

# 49. DO NOT OVER-REWRITE

Very important:

BetterHud already solves difficult problems.

Do not throw away working systems just because a cleaner design is imaginable.

Preserve and improve:

- bossbar interception
- HUD rendering semantics
- configuration compatibility
- pack generation concepts
- player head caching
- integrations
- popup behavior
- compass behavior
- animations
- existing useful APIs

Modernize incrementally.

Every large rewrite needs a concrete reason.

---

# 50. DEVELOPMENT ORDER

Follow approximately this order unless current upstream code suggests a better sequence.

## Phase 0: establish Glyph

- clone/fork latest BetterHud `dev`
- preserve git history
- rename project
- update metadata
- establish upstream remote
- document upstream strategy
- verify clean build before major edits

## Phase 1: platform cleanup

- Java 25
- Paper + Velocity only
- remove Fabric/mod code
- remove generic Spigot/Bukkit fallbacks where unnecessary
- clean Gradle modules
- clean dependencies
- use paperweight-userdev
- verify 26.1/26.2/26.3 architecture

## Phase 2: correctness/performance foundation

- PR #475 behavior
- packet deduplication
- renderer state caching
- scheduler cleanup
- remove obvious hot-path allocations
- dependency loading cleanup
- PR #502
- conditional flicker investigation
- profiling baseline

## Phase 3: packet/NMS cleanup

- refactor PlayerBossBar responsibilities
- isolate Netty interceptor
- reduce reflection
- version adapters
- preserve external bossbar compatibility
- benchmark component conversion
- ensure Folia correctness

## Phase 4: 26.3 renderer/shaders

- verify OpenGL
- verify Vulkan
- port relevant PR #500 work
- fix issue #479
- modern ShaderBackend
- 26.3 resource-pack shader changes
- post-effect API
- end_of_frame handling

## Phase 5: renderer modernization

- dirty dependency graph
- partial render invalidation
- typed values
- compiled static segments
- dynamic resources
- stronger caching
- immutable render snapshots

## Phase 6: new features

- row layout
- column layout
- stack layout
- improved flow
- dynamic images
- nine-slice backgrounds
- improved sizing/alignment
- useful CNP-inspired features

## Phase 7: ecosystem

- CraftEngine
- CustomNameplates
- TAB compatibility
- resource-pack conflict detection
- modern pack stacking/merge strategy
- Velocity handoff improvements

## Phase 8: future versions/platforms

- 26.4
- 27.x
- Fabric
- revisit new Mojang/Paper HUD/rendering APIs

---

# 51. EXPECTED OUTPUT

Do not just dump code.

Work through the project systematically.

During implementation:

1. Make commits grouped by logical change.
2. Keep the project buildable between major phases whenever possible.
3. Run tests/builds after significant changes.
4. Fix compile errors rather than commenting out features.
5. Preserve functionality unless intentionally removed.
6. Document intentional compatibility changes.
7. Add TODOs only for legitimate follow-up work, not as a substitute for implementing requested work.

Create:

- `README.md` updated for Glyph
- `GLYPH_UPSTREAM_NOTES.md`
- `GLYPH_ARCHITECTURE.md`
- `GLYPH_PERFORMANCE.md`
- migration notes from BetterHud where needed

---

# 52. FINAL DESIGN PRINCIPLE

Glyph should eventually be describable as:

**A modern, performance-focused evolution of BetterHud for modern Paper servers, with BetterHud compatibility, direct efficient packet rendering, cleaner 26.x architecture, better resource-pack/shader coexistence, and modern HUD layout/rendering features.**

Do not optimize for the smallest diff.

Do not optimize for maximum rewrite either.

Optimize for:

- correctness
- performance
- maintainability
- compatibility
- clean upstream tracking

Start by inspecting the latest BetterHud `dev` branch and produce an implementation plan based on what is actually there now. Then execute the plan rather than stopping after the analysis.