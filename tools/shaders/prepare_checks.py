"""Prepare reproducible offline shader checks using a matching official Minecraft client jar.

Usage: python prepare_checks.py CLIENT_JAR OUTPUT_DIR [VERSION=26.3]
No downloading or changing shader sources. Java driver needs matching LWJGL ShaderC jars/natives.
"""
from pathlib import Path
import re
import sys
import zipfile

client = zipfile.ZipFile(sys.argv[1])
output = Path(sys.argv[2])
output.mkdir(parents=True, exist_ok=True)
version = sys.argv[3] if len(sys.argv) > 3 else "26.3"
root = Path(__file__).resolve().parents[2]
constants = "\n".join(f"#define {k} {v}" for k, v in {
    "SHADER_VERSION": 4 if version == "26.3" else 3, "HEIGHT_BIT": 13, "MAX_BIT": 10,
    "ADD_OFFSET": 4095, "DEFAULT_OFFSET": 10, "LAYOUT_COUNT": 1,
    # 26.3 RenderPipelines.OIT_SNIPPET / LevelRenderer (wavelet rank 2).
    "OIT_WAVELET_RANK": 2, "OIT_COEFF_COUNT": 8, "OIT_COEFF_ATTACHMENT_COUNT": 2,
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
    for hide in (False, True):
        text = source_path.read_text(encoding="utf-8")
        if hide:
            text = text.replace("//HideExp", "")
        text = text.replace("#CreateConstant", constants).replace("#CreateLayout", "case 1: break;")
        text = re.sub(r"#Generate\w+", "", text)
        for name, defines in variants.items():
            expanded = expand(text)
            lines = expanded.splitlines()
            lines[1:1] = [f"#define {define}" for define in defines]
            (output / f"{version}_{name}_{'hide_exp' if hide else 'normal'}.{suffix}").write_text("\n".join(lines), encoding="utf-8")
print(f"Prepared {len(variants) * 4} shader inputs for {version}")
