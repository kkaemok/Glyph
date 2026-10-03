package online.libang.glyph.text

import online.libang.glyph.util.IntEntryMap

class HudTextData(
    val font: List<BackgroundKey>,
    val codepoint: IntEntryMap,
    val imageCodepoint: Map<String, Int>,
    val splitWidth: Int
)