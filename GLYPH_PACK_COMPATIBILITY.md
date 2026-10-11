# CraftEngine integration

Glyph follows BetterHud's original CraftEngine integration as of October 11, 2026.

When `merge-with-external-resources: true`, the async resource-pack cache event
reloads Glyph and adds the returned build ZIP directly to CraftEngine's external
ZIP set, or the returned directory to its external folder set. CraftEngine
compiles and distributes the combined pack using its own merge behavior.

Glyph does not pre-inspect CraftEngine's other inputs, reject cross-pack resource
collisions, select texture owners, filter textures/animation metadata, create a
private registration ZIP, or remove other entries from CraftEngine's sets. The
added `PackConflictInspector` and `ExternalPackRegistration` code and their
policy-specific tests have been removed. This reverses the previous collision
and texture-ownership policy at the user's request.

The source handler matches `UPSTREAM_BASE`, with implementation package names
and the reload warning adapted to Glyph. Keep `merge-with-external-resources`
enabled, regenerate CraftEngine's pack, and check for `Successfully merged with
CraftEngine.`. CraftEngine's collision diagnostics are handled by CraftEngine.

Glyph's generated pack still includes the Minecraft 26.3 shader overlays and
the minor-format metadata fix. Its own pack compiler remains separate from
CraftEngine's merging of external packs. CustomNameplates and dialogue source
files are not rewritten by this integration.

Earlier Glyph builds managed `plugins/Glyph/.cache/craftengine-registration.zip`.
The restored integration never registers that file. If it was manually added to
CraftEngine's configuration, remove that manual entry and use Glyph's build
output. A full server restart gives CraftEngine fresh cache input sets.

## Validation of 9d65a59d (2026-10-11, before the fork audit)

`gradlew.bat test build :glyph-paper-api:writeCompatibilityClasspath` passed in
2m 22s. All 79 remaining tests passed with zero failures, errors or skips. The
27 tests for the removed external inspection/registration policy were removed
with that policy; the eight popup lifecycle regressions remain.

Both distributions passed ZIP CRC, metadata, licensing, relocation, Java 25 and
shader-resource checks. The upstream handler source-parity check passed.
The original binary API verifier passed for Paper (82 types, 522 descriptors)
and Velocity (68 types, 459 descriptors), including its upstream-compiled
consumer. Removed external registration classes are absent from both JARs.

| Distribution | Bytes | SHA-256 |
| --- | ---: | --- |
| `Glyph-paper-0.1.0-SNAPSHOT.jar` | 35,355,509 | `557DA1C4B64E48594CE1294EFDFA875AC83B17EC88EA32C1B25BD272E462F504` |
| `Glyph-velocity-0.1.0-SNAPSHOT.jar` | 35,133,302 | `E4D3A4E5D955BE3E3DDA279E653FFA3E22DE529E4C57E6D232074DAA5A4D1E9B` |

The user reported that `ce reload all` restored animations, dialogues and external
bossbar text before receiving this rollback build. That recovery is evidence
about the previously deployed version, not validation of this new JAR on the
remote server.
