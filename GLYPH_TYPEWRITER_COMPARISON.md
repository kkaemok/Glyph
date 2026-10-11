# Typewriter version comparison and reload recovery

Compared the BetterHud API support commit `0b80ceee` (October 4, 2026)
with `ebe5266f` (October 11, 2026). Glyph runs only on Paper in this setup.

## Live result

The user confirmed that `ce reload all` restored HUD animations, Typewriter
dialogues and the other plugin's bossbar text with the existing installed JAR.
This happened before the CraftEngine rollback build was delivered. The recovery
points to stale generated-pack or reload state; it does not establish exactly
which server cache, generated layout or client resource state was stale.

## What stayed unchanged

No source differences were found in the neutral/Paper BetterHud API modules,
singleton initialization, Bukkit player registration/scheduling, CustomPopupEvent,
event unwrapping, custom_variable placeholder, popup lookup, PopupLayout,
layout/text/image renderers, font generation, shader generation/templates or
Paper dependency versions.

Rebuilt distributions contain byte-identical versions of all 88 canonical API
classes, five animation classes and 15 renderer classes checked. The API verifier
also passes: 82 Paper/neutral public types with 522 descriptors, and 68
Velocity/neutral types with 459 descriptors. An upstream-compiled consumer
exercises the shared singleton/managers, events, placeholders, Gson signatures
and popup calls. Its fixture popup verifies linkage, not remote visual rendering.

## Changes in the compared range

| Commit | Change | Relevance |
| --- | --- | --- |
| `2f0dea40` | Preserve external bossbar fonts; place unknown/custom-font titles in native slots with client centering. | Changes external bossbar transport, not Typewriter's popup event/variable path. |
| `4c8fa464` | Inspect external pack collisions and choose texture owners through a filtered registration ZIP. | Changes CraftEngine's registered pack and effective textures. |
| `89d51b01` | Remove stale/duplicate Glyph inputs during CraftEngine registration; restrict Velocity HUDs to opted-in backends. | CraftEngine registration affects Paper; Velocity changes do not apply here. |
| `ebe5266f` | Fix atomic popup publication/retirement and removal of marked iterators before replacement. | Both confirmed lifecycle defects already existed in `0b80ceee`; they do not identify the intervening regression. |

No intervening production change before `ebe5266f` modified Typewriter's event
or variable path, popup lifecycle/renderers or Paper player scheduler.

## Downloaded resource packs

The client's October 4 10:35 pack and October 11 pack have identical bytes for
all 39 selected resources: 25 popup_tw font JSON files, eight dialogue image
textures and six active Glyph 26.3 shader/include files. All 119 bitmap textures
referenced by the current dialogue fonts are present.

The earliest October 4 10:31 cache differs in ten dialogue text-font JSON files:
font2 coverage expanded from 40 to 96 providers. Image fonts and the text shader
match. Pack caches do not identify the running server commit or successful
dialogue, and the compared source has no corresponding font-generator change.
These offline checks alone cannot establish the live pack/layout pairing.

## Requested CraftEngine rollback

After the comparison, the user requested restoring BetterHud's original
CraftEngine behavior. Glyph now directly registers the build ZIP/directory from
its reload result. The handler matches UPSTREAM_BASE after implementation-package
and warning-name adaptations. External conflict inspection, texture ownership,
private registration ZIPs and input filtering were removed. Existing external
font preservation, popup lifecycle fixes and 26.3 pack metadata remain.

No CustomNameplates or dialogue configuration/resource files were modified.
See [CraftEngine integration](GLYPH_PACK_COMPATIBILITY.md) for build validation.

## Older source build

An optional comparison artifact was built successfully from `0b80ceee`:
`build/comparison/Glyph-paper-api-support-0b80ceee.jar` (35,351,890 bytes).
SHA-256: `265E34401770F9D1D159A1D418BA9D25F6961364EEA69D651F5BD4FA0957600E`.
This is a rebuild with available dependencies, not the archived October 4 binary.
Its Paper build and ZIP CRC passed; the full old test suite was not run.
The live recovery makes an A/B downgrade unnecessary for the reported symptoms.
