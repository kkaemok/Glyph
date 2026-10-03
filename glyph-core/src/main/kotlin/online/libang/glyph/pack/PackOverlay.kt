package online.libang.glyph.pack

import online.libang.glyph.util.PLUGIN

enum class PackOverlay(
    val overlayName: String,
    val minVersion: Int,
    val maxVersion: Int
) {
    V26_1("glyph_26_1", 84, 87),
    V26_2("glyph_26_2", 88, 96),
    V26_3("glyph_26_3", 97, 97)
    ;
    fun loadAssets() {
        PLUGIN.loadAssets(overlayName) { n, i ->
            val read = i.readAllBytes()
            PackGenerator.addTask(buildList {
                add(overlayName)
                addAll(n.split('/'))
            }) {
                read
            }
        }
    }
}