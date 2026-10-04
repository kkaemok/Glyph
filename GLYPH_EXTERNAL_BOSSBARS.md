# External bossbar font compatibility

## Audit of the proposed CNP patch

Audited against Glyph main `0b80ceee94574f7a3ccb7f285c91059b330252e9`
and `Glyph-CNP-bossbar-patch.zip` supplied by the user.

The diagnosis was correct. `ExternalBossBarText.measure` recursively assigned
Glyph's default font to every title node. Font keys select client resource-pack
providers; replacing a key changes the meaning and advance of its characters.
This destroys external images, shifted text, backgrounds and spacing glyphs.

The archive is insufficient as supplied:

- Its unified diff has bare `@@` markers. `git apply --check` rejects it with
  `patch with only garbage at line 4`.
- The proposed replacement passes a `Component` into `WidthComponent`, whose
  actual constructor requires a `TextComponent.Builder`. It would not compile.
- It retains Glyph's default-font width calculation for foreign font providers.
  That width cannot correctly center CNP images or signed spacing glyphs.
- Simply appending an unfonted component below Glyph's outer space-font root
  also changes its implicit font. An explicit vanilla-font wrapper is required
  for ordinary titles; the original title's nodes must remain untouched.

## What CustomNameplates sends

The audit inspected official CustomNameplates main at
`61d66eb7316f39e18a5ea6637688da6752a621eb`. The user's installed version and pack
have not been supplied; generated provider details can vary by version.

- [OffsetFont](https://github.com/Xiao-MoMi/Custom-Nameplates/blob/61d66eb7316f39e18a5ea6637688da6752a621eb/api/src/main/java/net/momirealms/customnameplates/api/feature/OffsetFont.java)
  maps private-use characters to signed advances.
- [ResourcePackManagerImpl](https://github.com/Xiao-MoMi/Custom-Nameplates/blob/61d66eb7316f39e18a5ea6637688da6752a621eb/backend/src/main/java/net/momirealms/customnameplates/backend/feature/pack/ResourcePackManagerImpl.java)
  generates offset bitmap providers, images/backgrounds and separate shift-font
  JSON providers. In this revision offsets use a bitmap with a signed height;
  assuming that all spacing is a modern `space` provider would also be wrong.
- [AdaptiveImageText](https://github.com/Xiao-MoMi/Custom-Nameplates/blob/61d66eb7316f39e18a5ea6637688da6752a621eb/api/src/main/java/net/momirealms/customnameplates/api/placeholder/internal/AdaptiveImageText.java)
  measures parsed text using CNP's advance manager, then surrounds it with a
  background prefix/suffix. Background drawing and cursor backtracking are
  interleaved with visible text; the resulting title need not have zero advance.
- [AdventureHelper](https://github.com/Xiao-MoMi/Custom-Nameplates/blob/61d66eb7316f39e18a5ea6637688da6752a621eb/api/src/main/java/net/momirealms/customnameplates/api/helper/AdventureHelper.java)
  surrounds strings with MiniMessage font tags and serializes the parsed
  Adventure tree to Minecraft component JSON.
- [BossBarSender](https://github.com/Xiao-MoMi/Custom-Nameplates/blob/61d66eb7316f39e18a5ea6637688da6752a621eb/backend/src/main/java/net/momirealms/customnameplates/backend/feature/bossbar/BossBarSender.java)
  sends that component on add/update-name packets and sends normal removal and
  progress packets. The wire title carries text, nested styles/font identifiers
  and children, **not numeric font advances**.

No CustomNameplates source has been copied into Glyph. No CNP dependency,
plugin configuration or user resource file is changed.

## Final behavior

`ExternalBossBarText.canMerge` checks inherited fonts and content throughout the
rendered title tree. Only single-line literal text using vanilla's implicit or
explicit `minecraft:default`, or Glyph's known default font, enters the existing
width-estimation merge path. The original component is appended unchanged below
a `minecraft:default` wrapper so it cannot inherit Glyph's space font. Font keys,
colors, decorations and children are never recursively rewritten. Italic text
no longer gets a fictitious extra cursor advance.

Foreign fonts, private-use characters (including supplementary planes), control
characters and client-resolved components such as translations, keybinds,
selectors and scores retain their full original titles in standalone slots.
Translations keep their keys, fallbacks, arguments and decorations. Their
resolution and centering are performed by Minecraft rather than guessed from
server strings. Unknown custom titles are never assigned a zero or default-font
width, and never enter `HudPlayer.additionalComponent`.

Paper and Velocity use the same `BossBarSlotPlan`:

1. The first `bossbar-line - 1` external bars may occupy the existing dummy
   slots. Those slots carry each complete original title, centered by the client.
2. The first mergeable remaining title shares Glyph's HUD slot, retaining its
   progress, color, overlay and flags.
3. Unmeasurable titles and overflow bars use their original UUIDs and complete
   titles. Such a title does not block a later ordinary title from merging.
4. Name changes can move a bar between the shared and standalone slots. Removal
   promotes the latest complete tracked state, including prior style updates.

The existing packet diffing, event-loop ownership, force-update behavior,
merge-mode disable/restore and close/restore paths remain in place.

### Limits and positioning

Glyph has no authoritative view of every client's effective font resources,
pack priority, provider conditions, Unicode font settings or language overrides.
Its existing `getWidth` table describes its own generated default font. It is
still an **estimate for ordinary vanilla-font text**, not a universal Minecraft
font measurement API. It cannot accurately measure arbitrary custom fonts from
component data alone. Reading one server-side pack would not prove the metrics
of the final pack actually loaded by each client.

This fix delegates custom font-aware measurement to the client, which owns the
effective providers. It adds no plugin-specific font names or codepoint tables.
A future merge of arbitrary fonts would need authoritative effective pack
metrics, provider order/conditions, inherited fonts, signed/fractional advances,
bold advances and client text resolution; an unknown width must still fall back.

Glyph's configured HUD line remains reserved. An unmeasurable title which would
previously have shared that line now occupies another bossbar row. Consequently
CNP configurations which assume a particular bossbar row may appear one row
lower. Existing dummy slots can carry custom titles without that extra row.
This patch does **not** promise arbitrary custom titles and Glyph share one row
or preserve an absolute screen Y coordinate despite a changed bar count.
Guessing a width or using zero would instead shift CNP and corrupt the cursor
position used to render Glyph's native HUD. CNP's own shaders and resources must
also be compatible with the client and final merged pack independently of this
server-side fix.

## Regression coverage

`ExternalBossBarTextTest` covers ordinary text, implicit-font inheritance under
Glyph's space root, nested explicit fonts and decorations, foreign root/child
fonts, BMP and supplementary private-use characters, signed spacing characters,
CNP-like backgrounds/shift fonts/images, translations/arguments/fallbacks and
client-resolved component types. Original subtrees retain identity and serialized
JSON; foreign fonts never reach Glyph's width lookup.

`BossBarSlotPlanTest` exercises the production shared allocator and title routing
for both transports: multiple bars, dummy slots, custom-title preservation,
ordinary merging behind custom titles, name updates changing eligibility,
style/progress/properties updates, removal/promotion, line changes and reset.

`BossBarPacketCodecTest` checks actual mapped Paper packet payloads for add,
update-name, update-style, update-progress, update-properties and remove, and
proves remapping retains the exact native title and all bossbar properties.

Run the full suite and both distributions with `./gradlew test build`.
CI uses the same full-suite selector and also checks the retained BetterHud API.
The tests do not replace a live CNP/Leaf/client rendering test.

### Verified results (2026-10-04)

- `gradlew.bat test build :glyph-paper-api:writeCompatibilityClasspath`: successful.
  All 64 tests passed, with zero failures/errors/skips: 60 core, 2 native Paper
  packet tests, 1 Paper integration inventory and 1 Velocity integration inventory.
  This includes 19 new regression tests. The final rerun after strengthening
  native font/decorations assertions also passed.
- Distribution ZIP integrity, MIT attribution, metadata, private library
  relocation, Java 25 class versions and modern shader presence: passed for both
  Paper and Velocity.
- `tools/api/verify_compatibility.py`: retained 82 original public Paper API types
  and 522 member descriptors, plus 68 Velocity types and 459 descriptors.
  An unchanged upstream-compiled consumer reached Glyph's shared managers,
  placeholders, Gson signatures, events and popup calls successfully.
- No live remote Leaf/CNP client session was available for this audit.

Built distributions in `build/libs`:

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `Glyph-paper-0.1.0-SNAPSHOT.jar` | 35,358,165 | `0104FC1FDCF4A35A249EE5372F0E0DD5CFAAACD34E6BC9D754117D63E8DE39F1` |
| `Glyph-velocity-0.1.0-SNAPSHOT.jar` | 35,127,837 | `6E706C3C02B173FFE63CA868226BF277D7C8497755EE21D54683F2DFDED4013F` |
