package online.libang.glyph.text

import com.google.gson.JsonArray

class HudTextArray(
    val file: String,
    val chars: JsonArray,
    val height: Double,
    val ascent: (Double) -> Int
)