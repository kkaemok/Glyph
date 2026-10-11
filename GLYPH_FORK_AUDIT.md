# Fork maintainability audit (2026-10-11)

Audited the fork-specific changes against UPSTREAM_BASE, using a normalized
source comparison to distinguish package/branding changes from behavior.
Reviewed render sampling/dependency caches, typed state, popup lifecycle,
external bossbar policy and both transports, pack compilation/output/CraftEngine
handoff, new layout/image features, command registration and API identity.
This is a source and automated-behavior audit, not a live server performance
benchmark or a claim that every inherited BetterHud line is defect-free.

## Findings corrected

| Finding | Impact | Correction |
| --- | --- | --- |
| Frame sampling used UpdateEvent.key as complete context identity. | PopupUpdateEvent wrappers can share a trigger key but contain different iterators. One popup could receive another's popup_index. Equal third-party event objects could also collapse distinct payloads. | Sample by placeholder-source and exact event-object identity, arguments and player. Repeated consumers of the same event still share one sample. No plugin-specific event adapter. |
| The legacy section marker was corrupted from U+00A7 to Korean U+CC20, including in tests. | Actual Minecraft section colour codes were left literal; ordinary Korean text could become formatting. | Restore U+00A7 using an explicit Unicode escape; test real colour/hex codes, Korean literal text and mixed ampersand/section input. |
| PackGenerator compiled/copied/hashed the asset map, then PackType reconstructed another CompiledPack from it. | Redundant full-map copying, validation and hashing on each pack build. Path regexes were also recompiled for every validation. | Carry one compiled pack through output generation and expose its files to ReloadState.Success. Precompile validation regexes. Existing deterministic-output/conflict tests remain. |
| SPACE_BETWEEN multiplied two Int values despite accepting valid large row widths. | Intermediate overflow produced negative/incorrect child offsets. | Use Long arithmetic for spacing distribution and convert the bounded result back to Int. |
| Command root was hud, with betterhud/bh aliases, and its permission prefix was hud while metadata/subcommands used glyph.*. | No glyph root; help/root permissions used inconsistent names. | Primary glyph command; betterhud, bh and hud aliases. Use glyph root/permission prefix; declare OP-default root permission with glyph.help child. Subcommands keep their own permission checks. |

No CustomNameplates or dialogue configuration/resource files were modified.
No new optional-plugin dependency, plugin-name check or popup-ID exception was
introduced. The frame/context and serializer corrections apply to every caller.

## Compatibility code retained intentionally

- Original BetterHud API names and one shared live singleton avoid a copied API
  facade, per-plugin adapters or duplicate player state. Public Gson/BetterCommand
  identities remain shared where inherited descriptors expose them.
- External components keep their font trees. Unknown/custom font advances and
  client-resolved text use native client centering. Components alone cannot supply
  resource-pack advances. The decision uses component properties, not CNP identity.
  Ordinary default-font measurement remains an estimate, including if another
  pack changes minecraft:default.
- Popup publication/removal coordination and delayed player task startup fix
  general concurrency/construction failures. They contain no Typewriter branch.
- Velocity ownership is a generic configured backend scope, avoiding overlapping
  proxy/backend HUDs. Paper remains independently usable.
- Minecraft-version shader templates, pack minor-format bounds and NMS adapters
  belong at the version boundary. The 97.1 correction addresses format semantics,
  rather than assuming one plugin's ZIP content.
- Dynamic image tables and nine-slice variants compile at pack generation time;
  arbitrary runtime identifiers select a bounded table and fallback. Native layout
  caching is explicit opt-in; undeclared legacy inputs continue polling.
- CraftEngine follows the original direct ZIP/directory handoff. The recently
  removed external collision/texture-owner/filtering layer stays removed.

## Remaining maintenance concerns

Paper and Velocity still duplicate some reconciliation and per-field packet
publication logic. Slot selection, state ordering and font eligibility are shared,
but the native packet/component types differ. A future extraction should move a
tested state transition model, rather than add a broad adapter abstraction solely
to reduce similar-looking lines.

Velocity's ownership scope serializes callbacks across players, and its leave
callback saves player data synchronously before replacement. This preserves
ordering but slow storage can delay other ownership transitions. It remains a
performance review candidate; safe per-player serialization requires preserving
reload/disconnect and persistence ordering. This audit did not change that design.

Dependency declarations promise that all changing inputs are native state values.
An incorrect declaration can freeze legacy/time-dependent content; the compiler
does not infer all third-party placeholder dependencies. Keep this explicit
contract. Packet-send caches also cannot confirm eventual client delivery from
an asynchronous enqueue. Live reconnect and load profiling remain necessary.

## Validation

Three new sampling/geometry regressions failed against the pre-fix code. The
corrected legacy tests also exposed the corrupted marker before the serializer
fix. The distribution-level command probe exposed the old help/root permission
prefix before it was corrected. It builds the actual CommandManager tree from the
shaded JAR and checks all four labels for parsing, help, permission denial and tab
completion; it does not substitute a toy command tree.

`gradlew.bat test build :glyph-paper-api:writeCompatibilityClasspath` passed in
2m 33s. All 84 tests passed with zero failures, errors or skips. The API verifier
passed for Paper (82 public types, 522 descriptors) and Velocity (68 types,
459 descriptors); the upstream-compiled consumer and actual distributed command
tree probe passed. Both JARs passed ZIP CRC, metadata, attribution, relocation,
Java 25 and shader-resource checks.

| Distribution | Bytes | SHA-256 |
| --- | ---: | --- |
| `Glyph-paper-0.1.0-SNAPSHOT.jar` | 35,355,345 | `B89064A1C0DCE31EEBA49AE40E015C990EBC075AADF4591A3083FC873021E812` |
| `Glyph-velocity-0.1.0-SNAPSHOT.jar` | 35,133,091 | `FDB7A6BA5D56313FCCC413113C12E2E62CC8CFA84A47C41D4FA0E38549DA1DCC` |

The removed redundant pack pass is established by code flow, not a measured
whole-server speedup. Live server/client retesting of this build remains separate
from the automated checks.
