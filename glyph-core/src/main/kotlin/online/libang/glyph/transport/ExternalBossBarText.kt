package online.libang.glyph.transport

import online.libang.glyph.api.GlyphAPI
import kr.toxicity.hud.api.component.WidthComponent
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
import org.jetbrains.annotations.ApiStatus

/**
 * Default-font titles retain the historical width estimate. Custom fonts, private-use glyphs
 * and client-resolved component types must be centered by the client in a standalone slot:
 * an Adventure component contains no resource-pack glyph advances.
 */
@ApiStatus.Internal
object ExternalBossBarText {
    val DEFAULT_FONT: Key = Key.key("minecraft", "default")

    fun canMerge(component: Component, glyphFont: Key): Boolean {
        fun visit(c: Component, inheritedFont: Key): Boolean {
            val font = c.font() ?: inheritedFont
            if (font != DEFAULT_FONT && font != glyphFont) return false
            when (c) {
                is TextComponent -> if (c.content().codePoints().anyMatch {
                    Character.getType(it) == Character.PRIVATE_USE.toInt() || Character.isISOControl(it)
                }) return false
                // Translation, argument substitution, language overrides and locale are resolved by the client.
                is TranslatableComponent -> return false
                else -> return false
            }
            return c.children().all { visit(it, font) }
        }
        return visit(component, DEFAULT_FONT)
    }

    fun measure(component: Component): WidthComponent? {
        val api = GlyphAPI.inst()
        return measure(component, api.defaultKey, api::getWidth)
    }

    internal fun measure(component: Component, glyphFont: Key, glyphWidth: (Int) -> Int): WidthComponent? {
        if (!canMerge(component, glyphFont)) return null
        fun decorated(parent: Boolean, state: TextDecoration.State) = when (state) {
            TextDecoration.State.TRUE -> true; TextDecoration.State.FALSE -> false; TextDecoration.State.NOT_SET -> parent
        }
        fun width(c: Component, inheritedBold: Boolean): Int {
            val bold = decorated(inheritedBold, c.decoration(TextDecoration.BOLD))
            val text = (c as TextComponent).content()
            var total = 0
            var offset = 0
            while (offset < text.length) {
                val point = text.codePointAt(offset); offset += Character.charCount(point)
                total += (if (point == 32) 4 else glyphWidth(point) + 1) + if (bold) 1 else 0
            }
            for (child in c.children()) total += width(child, bold)
            return total
        }
        // Leave the original tree untouched. Otherwise unfonted nodes inherit Glyph's outer space font.
        return WidthComponent(Component.text().font(DEFAULT_FONT).append(component), width(component, false))
    }
}
