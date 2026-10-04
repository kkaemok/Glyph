# CraftEngine pack collision policy

## Bossbar texture investigation

Glyph's `common-resources/bars.png` and the official CustomNameplates source
atlas at revision `61d66eb7316f39e18a5ea6637688da6752a621eb` are byte-identical:
256×256 pixels, 1,833 bytes, SHA-256
`c15bfc77235fe522043d6e7581425f8e5ca486aef7f46012740980e01f42aa6d`.

Glyph's `ShaderManagerImpl` draws that atlas into a new ARGB image, leaving a
256×10 color band blank. CNP's
[ResourcePackManagerImpl](https://github.com/Xiao-MoMi/Custom-Nameplates/blob/61d66eb7316f39e18a5ea6637688da6752a621eb/backend/src/main/java/net/momirealms/customnameplates/backend/feature/pack/ResourcePackManagerImpl.java)
clears the corresponding 182×10 bossbar region in the source image. The color
order is pink, blue, red, green, yellow, purple, white. Their default configurations
both hide yellow. Their output PNG bytes can differ even for that same color:
encoding, invisible RGB and Graphics2D's handling of partially transparent
source pixels differ. The actual shipped-atlas regression fixtures produced
2,088 visible-RGBA and 50,420 invisible-RGB pixel differences; the inspector
therefore selected CNP's atlas as the explicit owner rather than claiming exact
pixel equivalence.

Modern clients use bossbar sprites. Glyph's `background.png` and CNP's yellow
background/progress sprites are all 182×5, fully transparent images. Glyph uses
transparent black (134-byte PNG); CNP uses transparent white (144-byte PNG).
Those sprites have the same visible pixels despite different bytes. The new
comparison accepts them as equivalent.

No CNP source, config or resource pack is modified, and no CNP dependency or
plugin-specific namespace rule is added.

## Inspection and ownership

`PackConflictInspector` reads ZIPs and folders, resolves each pack's active
overlays using the full major/minor format, and checks the effective resource
stack. Its `Inspection` reports equivalent PNGs and selected external owners.

| Collision | Decision |
| --- | --- |
| Byte-identical resource | Accept |
| Ordinary PNG with matching dimensions, decoded ARGB pixels and animation metadata | Accept; fully transparent RGB differences are ignored |
| Different ordinary PNG or its animation metadata in Glyph and one external owner | External pack owns the logical texture; omit Glyph's PNG and metadata from its registration copy |
| Different PNGs from multiple external owners | Fail; no arbitrary plugin ordering |
| Core shaders/includes/program JSON, font JSON/providers, post effects, other non-texture resources | Fail on different bytes |
| Font texture, or bitmap-font/effect/shader texture dependency outside the usual font folder | Fail on different bytes |
| Duplicate/invalid overlay declaration, inverted bounds, shared overlay directory with different bounds | Fail |
| Texture override supplied only by an external overlay, without a base asset | Fail; removing Glyph's copies could leave other versions without an owner |

Font bitmap identifiers are resolved relative to `textures/`, including a
legitimate nested directory named `textures`. Effect input `location`/`texture`
references are also protected. Dependencies are collected from base and overlay
JSON, so inactive critical definitions cannot lose their textures in a filtered
registration copy. Overlay directory spelling follows Minecraft's directory
codec, including valid mixed case.

PNG equivalence is not used to soften font/provider or shader/post-effect checks.
`CompiledPack.Builder` also remains strict: this exception applies to external
CraftEngine registration, not arbitrary merging inside Glyph's pack compiler.

## CraftEngine registration

The actual `AsyncResourcePackCacheEvent` handler calls `ExternalPackRegistration`
with CraftEngine's `externalFolders()` and `externalZips()` sets. The compiled
26.9.1 `PackCacheData` getters return those same mutable sets.

If all collisions are identical/equivalent, the original Glyph output is
registered. When an ordinary texture needs one external owner, a deterministic
ZIP is written to `plugins/Glyph/.cache/craftengine-registration.zip`. It retains
Glyph's metadata, overlays, shaders, fonts and unrelated assets, and omits only
the owned texture and its metadata from Glyph's base/overlay copies. The external
pack remains registered and supplies the texture. Original Glyph and external
packs remain unchanged.

Regeneration excludes the previous Glyph registration from conflict input,
replaces that cache entry, and can switch between folder, original ZIP and
filtered ZIP output. A genuine critical conflict removes the previous Glyph
entry and reports failure while retaining other plugins' entries.

The external owner is explicit in Glyph's log. When the two plugins hide
different legacy bossbar colors, the selected external atlas determines the
legacy appearance; this does not combine both masks. Modern sprites remain
independent resources. Later-generated or later-registered CraftEngine assets
still require final-pack verification; this check inspects the resources
available at Glyph's cache callback for the server's target format.

## Regression results (2026-10-04)

Added 19 tests across `PackTextureConflictTest` and `ExternalPackRegistrationTest`.
The fixtures use Glyph's actual shipped atlas and reproduce both plugins'
generation behavior without bundling CNP code/assets.

Coverage includes same/different-color CNP atlases, equivalent modern sprites,
ordinary texture dimensions/animation ownership, font/effect texture dependencies,
strict shader/font/post-effect failures, ambiguous external owners, overlay
bounds/directory rules, overlay-only ownership, ZIP/folder registration, retained
Glyph shaders/fonts/metadata, unchanged source packs, regeneration and failure
cleanup. Registration assertions exercise the same helper called by the real
CraftEngine event handler, rather than only checking that inspection succeeds.

`gradlew.bat test build :glyph-paper-api:writeCompatibilityClasspath` succeeded.
All 83 tests passed with zero failures, errors or skips. Both distributions passed
ZIP integrity, MIT attribution, metadata, relocation, Java 25 class-version and
modern-shader presence checks. A live remote CraftEngine/Leaf pack-generation
session was not available.

| Distribution | Bytes | SHA-256 |
| --- | ---: | --- |
| `Glyph-paper-0.1.0-SNAPSHOT.jar` | 35,374,898 | `17EEF4E29309B2C2BA5E93C621D36BB8E1D21E0BF2AA15B6E89C8F77023F5C5B` |
| `Glyph-velocity-0.1.0-SNAPSHOT.jar` | 35,143,703 | `0596FFB0087CC336F3B9E653AFC182F5AF7FF8E0DEDFAB8FBFB4B1E733D22217` |
