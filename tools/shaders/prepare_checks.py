"""Prepare reproducible offline shader checks using a matching official Minecraft client jar.

Usage: python prepare_checks.py CLIENT_JAR OUTPUT_DIR [VERSION=26.3]
No downloading or changing shader sources. Java driver needs matching LWJGL ShaderC jars/natives.
"""
from pathlib import Path
import argparse
import re
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("client_jar")
parser.add_argument("output_dir")
parser.add_argument("version", nargs="?", default="26.3")
parser.add_argument("--profile", choices=("ordinary", "wave", "rainbow", "tiny_rainbow", "all", "static_scale", "extensions"), default="all")
parser.add_argument("--template-dir", type=Path, help="Optional templates for a regression negative control")
args = parser.parse_args()
client = zipfile.ZipFile(args.client_jar)
output = Path(args.output_dir)
output.mkdir(parents=True, exist_ok=True)
version = args.version
root = Path(__file__).resolve().parents[2]
pipeline_constants = {"OIT_WAVELET_RANK": 2, "OIT_COEFF_COUNT": 8, "OIT_COEFF_ATTACHMENT_COUNT": 2}
properties, static_scale = {
    "ordinary": (0, False), "wave": (1, False), "rainbow": (2, False), "tiny_rainbow": (4, False),
    "all": (7, True), "static_scale": (0, True), "extensions": (7, True),
}[args.profile]
constants = "\n".join(f"#define {k} {v}" for k, v in {
    "SHADER_VERSION": 4 if version == "26.3" else 3, "HEIGHT_BIT": 13, "MAX_BIT": 10,
    "ADD_OFFSET": 4095, "DEFAULT_OFFSET": 10, "LAYOUT_COUNT": 1,
    # 26.3 RenderPipelines.OIT_SNIPPET / LevelRenderer (wavelet rank 2).
    **pipeline_constants,
    "GLYPH_SHADER_PROPERTIES": properties, "GLYPH_STATIC_SCALE": int(static_scale),
}.items())

def expand(text):
    def include(match):
        name = match.group(1).removeprefix("minecraft:")
        source = client.read("assets/minecraft/shaders/include/" + name).decode()
        # The 26.1/26.2 Mojang import preprocessor strips versions from included files.
        source = re.sub(r"^\s*#version[^\n]*", "", source, flags=re.MULTILINE)
        return expand(source)
    return re.sub(r"#(?:include|moj_import)\s+<([^>]+)>", include, text)

variants = {"world": [], "gui": ["IS_GUI"], "see_through": ["IS_SEE_THROUGH"],
    "grayscale": ["IS_GUI", "IS_GRAYSCALE"]}
if version == "26.3":
    variants.update({"oit_accumulate": ["OIT", "OIT_ACCUMULATE"],
        "oit_depth": ["OIT", "OIT_DEPTH_BOUNDS", "OIT_ALPHA_ONLY"],
        "oit_transmittance": ["OIT", "OIT_TRANSMITTANCE", "OIT_ALPHA_ONLY"]})
for suffix in ("vsh", "fsh"):
    source_path = root / "common-resources" / (f"shader/26.3/text.{suffix}" if version == "26.3" else f"text.{suffix}")
    if args.template_dir:
        source_path = args.template_dir / f"text.{suffix}"
    for hide in (False, True):
        text = source_path.read_text(encoding="utf-8")
        if hide:
            text = text.replace("//HideExp", "")
        layout = f"case 1: property = {properties};"
        if static_scale:
            layout += " pos.xy *= uiScreen;"
        layout += " break;"
        if args.profile == "extensions":
            # Equivalent to an API tag supplier that requires the shared Globals environment.
            layout = "case 1: break;"
            text = text.replace("#GenerateOtherMainMethod", "vertexColor.x += GameTime;" if suffix == "vsh" else "")
        text = text.replace("#CreateConstant", constants).replace("#CreateLayout", layout)
        text = re.sub(r"#Generate\w+", "", text)
        for name, defines in variants.items():
            expanded = expand(text)
            lines = expanded.splitlines()
            # A compiled pack already contains Glyph's constants. Supply the
            # defines that Minecraft's OIT pipeline contributes at draw time.
            pipeline_defines = [f"#define {key} {value}" for key, value in pipeline_constants.items()
                if not re.search(rf"^\s*#define\s+{key}\b", expanded, re.MULTILINE)]
            lines[1:1] = [f"#define {define}" for define in defines] + pipeline_defines
            (output / f"{version}_{name}_{'hide_exp' if hide else 'normal'}.{suffix}").write_text("\n".join(lines), encoding="utf-8")
print(f"Prepared {len(variants) * 4} shader inputs for {version}")
