# Shared text shader compatibility (26.3)

The reported issue is Essential menu buttons failing on Vulkan while Glyph's
HUD itself renders normally. OpenGL with shader packs was subsequently confirmed
working; no OpenGL HUD regression is established by this report.

## Evidence

The client's log reports `Missing uniform Globals (should be UNIFORM_BUFFER)`
from Essential's `UFontRenderer.render`, through its UI font renderer and overlay
texture baking. Inspection of the installed UniversalCraft 26.3 build 534 shows
that this pass binds projection, dynamic transforms, fog and textures, but omits
Globals. Its glyph draw path selects Minecraft's GUI text pipeline.

Minecraft 26.3's text pipeline allows Globals. Vanilla's text vertex shader does
not read that block, whereas Glyph's shared text shader referenced ScreenSize
and GameTime even for layouts that did not need either. Vulkan's compiled shader
retained the uniform block, exposing Essential's incomplete pass bindings.

The inspected pack downloaded on 2026-10-11 had no shader properties enabled and
no static scaling. It therefore had no functional need for Globals. BetterHud's
current upstream 26.3 shader also includes Globals; the uniform alone does not
establish a Glyph-specific difference from every BetterHud version.

## Implementation

ShaderFeatures aggregates shader property bits and static scaling from the
registered HUD layouts on each pack compilation. The 26.3 template uses those
compile-time constants to include only configured wave/rainbow branches and
ScreenSize calculations. Ordinary positioning, opacity, relative scaling,
server-driven image/timeline animation and text rendering require no Globals.
The normal text pipeline, font keys and external bossbar handling are unchanged.

Shader API tag extensions conservatively retain the complete uniform/effect
environment. This preserves extensions that read Globals or change a property
at runtime. There is no Essential-name check, client-mod dependency, or disabling
of configured HUD effects. Legacy 26.1/26.2 templates retain their existing behavior.

Existing pristine 26.3 vertex templates are upgraded using the normalized SHA-256
fingerprints of the two previously shipped stock revisions. Both LF and CRLF
files qualify. Custom shader files, including stock files with user edits, are
preserved. For customized templates, port the conditional feature guards into
the customized shader before expecting the dependency removal.

## Verification and limitations

Regression tests cover feature aggregation/removal, static and relative scaling,
API extension fallback, new template installation, stock LF/CRLF upgrade,
preservation of edited shaders and idempotent reloads.

The offline shader check now inspects SPIR-V block names, in addition to compiling
the text variants. Ordinary, wave, rainbow, tiny rainbow, combined effects,
static scaling and extension profiles each pass OpenGL and Vulkan compilation.
The ordinary profile declares no Globals. Required profiles retain it. The old
stock template fails the ordinary profile's absent-Globals assertion.

Full `test build` passed in 2m 28s: 93 tests, zero failures/errors. The actual
shader sources emitted by the shaded Paper distribution passed 392 OpenGL/Vulkan
compilation and uniform-contract checks. Stock template checks also passed 392
checks; the old stock template negative control failed all 28 vertex assertions
as expected. Both distribution archives passed integrity/metadata checks, and
the upstream-compiled BetterHud API consumer and command-tree checks passed.

Paper JAR SHA-256:
`FE7FC6E581CB3F16399A9F05012FD7F2EAF181BCFB0D931052AE10ABFDB6560D`.

This removes Glyph's unnecessary uniform dependency for the reported pack. It
does not supply a client render-pass binding: Essential still needs to bind
Globals when a configured GPU effect, static scaling or shader extension requires
that block. Compilation checks do not replace a live Essential/Vulkan client test.

## Install

Replace the Paper plugin with `build/libs/Glyph-paper-0.1.0-SNAPSHOT.jar` and restart
the server. After Glyph rebuilds its pack, run `ce reload all` to distribute the
updated merged pack. Reconnect and accept the refreshed server resource pack,
then test Essential's pause menu and quick-access menu on Vulkan. CNP, dialogue
and CraftEngine resource/configuration files do not need rewriting.

## Reproduce shader contract checks

Supply the matching official client JAR and LWJGL ShaderC runtime/natives. For example:

```text
python tools/shaders/prepare_checks.py CLIENT.jar OUTPUT 26.3 --profile ordinary
java -cp CLASSES_AND_LWJGL_CP ValidateShaders OUTPUT false
```

Use `true` for the profiles that require Globals. ValidateShaders.java must first
be compiled against the LWJGL and ShaderC libraries. The check forces GLSL 450
for offline SPIR-V generation; it does not emulate the client's GL driver or
Essential's actual draw pass.
