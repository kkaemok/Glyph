# Async popup publication and retirement

## Failure

`PopupImpl.show0()` previously published an empty group using `computeIfAbsent`
before evaluating conditions and constructing the popup iterator. During that
work, `HudPlayerImpl.renderUpdate()` could see the empty group and remove it.
`show0()` then added the iterator to the detached group and returned an updater.
Updating that iterator succeeded, but the player never rendered it. No exception
was necessary. A similar race existed between adding to an existing group and
the renderer deciding to retire it.

There was also a deterministic replacement failure: `addIterator()` did not
actually remove marked iterators from its sorted set. When a dialogue was hidden
and its replacement shown before a render tick, the old entry still occupied the
same index and the set silently rejected the new iterator. Unique popups also
mistook the marked entry for an active duplicate.

This is relevant to integrations such as Typewriter that invoke the popup API
from another thread. A successful updater alone did not establish that its
group remained attached. The race does not depend on Typewriter classes or
resource-pack content.

## Fix

`PopupGroupLifecycle` coordinates publication and retirement using per-key map
computations on the player's `ConcurrentHashMap`. `PopupImpl` constructs its
iterator before publishing it; adding the iterator and publishing/reusing the
group happen together. The renderer samples and retires groups using the same
per-key operation, removing only groups with no iterators. An empty rendered
frame alone no longer detaches an active or queued group.

Updater removal marks its own iterator; it does not remove a group by name,
which could detach a replacement. The removal marker is volatile for visibility
between API callers and the render thread. Adding an iterator purges marked
entries before assigning slots or checking uniqueness. Active unique entries
continue to prevent duplicates. Public BetterHud API signatures,
event sources, dialogue variables, layouts and resource-pack files are unchanged.

## Validation

Eight `PopupGroupLifecycleTest` regressions exercise real group scheduling with
controlled iterators: blocked asynchronous publication, concurrent retirement
and show, active empty frames followed by updates, removal with independent
groups, removal of an old iterator after a replacement has been published,
immediate ordinary and unique replacements, and active unique duplicate rejection.
The publication test explicitly pauses setup so the original ordering would
expose an empty group to the renderer. An offline negative control of the original
publication ordering also reproduced a live iterator in a detached group.
Both immediate-replacement tests failed against the original sorted-set cleanup
before the fix was applied.

The installed BetterHud extension's popup helper passes a `CustomPopupEvent`
through `BukkitEventUpdateEvent` directly to `Popup.show()`, and supplies both
`typewriter_text` and its newer `text` alias. The client's downloaded CraftEngine
pack contains the dialogue image/font resources. Neither finding establishes a
live render result; these are confirmed code defects and plausible
causes of the reported silent Typewriter failure. Retesting the rebuilt JAR on
the user's server is still required.

## Build result (2026-10-11)

`gradlew.bat test build :glyph-paper-api:writeCompatibilityClasspath` succeeded.
The final reports contain 106 tests with zero failures, errors or skips,
including all eight new lifecycle tests. Both distributions passed ZIP CRC,
plugin metadata, licensing, relocation, Java 25 and shader-resource checks.
All 119 bitmap textures referenced by the client's 25 dialogue fonts are present
in its downloaded pack.

| Distribution | Bytes | SHA-256 |
| --- | ---: | --- |
| `Glyph-paper-0.1.0-SNAPSHOT.jar` | 35,381,419 | `07CEECB0DC4BFDA7A98B7A0E9564338A077023F12A76FB3F1B18754C77371852` |
| `Glyph-velocity-0.1.0-SNAPSHOT.jar` | 35,156,049 | `8B00CADE155BA70B0C698C1A677B5D0341676CFD0C535B80C0BBDD3AFE327E19` |
