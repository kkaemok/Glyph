package online.libang.glyph.pack

import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Actual shipped atlas, modified as Glyph and CNP generate it; no CNP code or binary assets bundled. */
internal object BossBarPackFixtures {
    const val BARS = "assets/minecraft/textures/gui/bars.png"
    const val SPRITE = "assets/minecraft/textures/gui/sprites/boss_bar/yellow_background.png"
    const val SHADER = "glyph_26_3/assets/minecraft/shaders/core/text.vsh"
    const val FONT = "assets/glyph/font/hud.json"
    val metadata: ByteArray get() = PackMeta.default.toByteArray()

    private fun atlas() = javaClass.getResourceAsStream("/bossbar-fixtures/bars.png")!!.use { ImageIO.read(it) }
    fun glyphBars(color: Int = 4): ByteArray {
        val source = atlas()
        val output = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        output.createGraphics().apply {
            if (color > 0) drawImage(source.getSubimage(0, 0, source.width, color * 10), 0, 0, null)
            val bottom = (color + 1) * 10
            drawImage(source.getSubimage(0, bottom, source.width, source.height - bottom), 0, bottom, null)
            dispose()
        }
        return png(output)
    }
    fun cnpBars(color: Int = 4): ByteArray {
        val output = atlas()
        // CNP erases only the 182px bossbar portion, retaining the rest of the source atlas.
        output.createGraphics().apply {
            composite = AlphaComposite.Clear
            fillRect(0, color * 10, 182, 10)
            dispose()
        }
        return png(output)
    }
    fun image(width: Int = 182, height: Int = 5, argb: Int = 0): ByteArray =
        png(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
            for (y in 0 until height) for (x in 0 until width) setRGB(x, y, argb)
        })
    fun png(image: BufferedImage): ByteArray = ByteArrayOutputStream().use {
        check(ImageIO.write(image, "png", it))
        it.toByteArray()
    }
    fun glyphFiles(texture: ByteArray = glyphBars()): Map<String, ByteArray> = mapOf(
        "pack.mcmeta" to metadata, BARS to texture, SHADER to "Glyph shader".toByteArray(),
        FONT to """{"providers":[]}""".toByteArray())
}
