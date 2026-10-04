# Glyph configuration extensions

Existing absolute layouts remain the default. These examples extend the usual
BetterHud `images`, `layouts` and `huds` files; they are not standalone pack files.

## Default HUD and resource pack

The shipped `default-hud` list enables `test_hud`. The client must also load the
generated pack (`plugins/Glyph/build.zip` with the default output path). Pack
self-hosting is disabled by default. For CraftEngine distribution, keep
`merge-with-external-resources: true` in Glyph's config, regenerate CraftEngine's
pack, check for `Successfully merged with CraftEngine.`, and accept the updated
pack on the client. Other distributors must include Glyph's generated assets in
their final pack; distributing a different pack alone does not install them.

On 26.3 the final pack's `glyph_26_3` overlay must include format 97.1. An old
`max_format: [97,0]` skips the HUD shaders even when the fonts are present. Install
the fixed jar and regenerate the distributor's pack so its content/hash changes;
replacing the jar alone does not update an already downloaded merged pack.

## External bossbar titles

With `merge-boss-bar` enabled, ordinary literal titles retain the existing HUD
merge behavior. Custom-font/private-use titles, including CustomNameplates HUDs,
keep their full font tree in client-centered standalone bossbar slots. They may
occupy existing dummy slots; a custom title at Glyph's HUD line needs an extra
row. Glyph's `bossbar-line` stays reserved. No CNP config/resource changes are
required by this fix, but a CNP layout which assumes a fixed bossbar row may
appear lower. See [the font and centering audit](GLYPH_EXTERNAL_BOSSBARS.md) for
the width-estimation limits and regression coverage.

## Image libraries and selection

An `images/icons.yml` declaration imports all PNGs under `assets/skills`:

```yaml
skills:
  type: directory
  directory: skills
```

`assets/skills/fire.png` becomes image `skills/fire`. Keep resource filenames
lowercase. Paths must remain inside `assets`; duplicate resources are diagnosed.
In a layout, use a pattern and an existing fallback:

```yaml
skill_row:
  layout: row
  gap: 4
  padding: 2
  dependencies: [skill_id, health]
  images:
    icon:
      source: "skills/[state:skill_id]"
      fallback: skills/unknown
      order: 0
  texts:
    health:
      name: entity_font
      pattern: "HP [state_number:health]"
      align: left
      order: 1
```

Set `skill_id` to `HudValue.Text("fire")` and `health` to a numeric value through
the API. `entity_font` is included in the default configuration; adapt the font
name when using a custom pack. Create an `unknown.png` fallback. Patterns may
contain existing placeholders too, with `dependencies` omitted for polling.

By default Glyph compiles all image identifiers matching the pattern's static
prefix into that layout's font. Optional `candidates: [skills/fire, skills/ice]`
limits this set. No texture/font is generated at runtime. Missing identifiers
use the fallback; warnings are deduplicated and capped at 64 per renderer.

Declare every changing native value in `dependencies`. An unrelated state
change then leaves this segment cached. Animations, changing external
placeholders and followed players require polling unless fully modeled by those
dependencies. HUD and popup renderers both support dynamic images.

## Flow geometry

`layout` accepts `absolute`, `row`, `column` and `stack`. Children sort by numeric
`order`; equal values preserve image/text/head declaration order.

- Rows use measured output widths. Hidden zero-width children consume no gap.
  `gap`, `padding`, `min-width`, `max-width` and `justify` configure the row.
  Justification accepts `start`, `center`, `end`, `space-between`.
- Columns compile vertical positions into font ascents. `cell-height` overrides
  the inferred image/head height or text line-height. Hidden children retain
  their reserved cells; changing the font geometry requires a pack rebuild.
- Stacks overlay children inside padding, with existing `x`, `y` and `layer`
  controlling placement and order. Absolute positioning preserves old behavior.

Width limits constrain the container, not glyph clipping; oversized content can
overflow. Use text split-width/line configuration and bounded asset variants.
Keep text alignment left in a row unless deliberate per-child offsets are wanted.

## Nine-slice panels

One source image can generate several bounded sizes at pack compilation:

```yaml
panel:
  type: nine_slice
  file: ui/panel.png
  border: {left: 4, top: 4, right: 4, bottom: 4}
  mode: tile
  sizes:
    small: {width: 80, height: 24}
    large: {width: 160, height: 48}
```

This creates `panel/small` and `panel/large`. Select with
`source: "panel/[state:panel_size]"`, `fallback: panel/small` and a Text state
value such as `large`. Width changes participate in row measurement. `stretch`
uses nearest-neighbour scaling; `tile` repeats the edges/centre. Both preserve
fixed corners. Targets must fit their borders; declarations allow at most 128
variants, and an output image at most 16M pixels. A single size can instead use
top-level `width`/`height` without `sizes`.

## Packs and ecosystem

Glyph's Paper requests retain unrelated packs and use one stable generated-pack
identity across hash changes. This does not make shared font or shader overrides
composable. Merge packs with shared resources through one compiler; resolve
different content at the same effective path explicitly.

For CraftEngine ownership, enable the existing `merge-with-external-resources`
option and let CraftEngine distribute the final pack. Disable Glyph self-hosting
when CraftEngine owns distribution. The async cache event validates Glyph against
declared CraftEngine resource folders and registered external packs, including
active overlays. Later-generated CraftEngine assets still require final-pack QA.

CustomNameplates and TAB retain their own namespaces/fonts. Test their bitmap
fonts and offsets with Glyph using the matrix in GLYPH_PERFORMANCE.md. External
bossbar text currently retains BetterHud's default-font width measurement; custom
font bossbar names need particular live attention. Core shader overrides remain
version-sensitive even after offline compilation succeeds.

`assets/minecraft/post_effect/end_of_frame.json` has one effective owner at each
pack priority. Conflicting definitions fail validation. Glyph does not concatenate
arbitrary post-effect graphs. Player post-effect keys use Paper's 26.3 API on the
player's owning thread and require matching client pack assets.
