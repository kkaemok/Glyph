package online.libang.glyph.transport

import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.api.component.WidthComponent
import online.libang.glyph.api.player.HudPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
import org.jetbrains.annotations.ApiStatus

/** Preserve upstream default-font measurement while avoiding a code-point array per text node. */
@ApiStatus.Internal
object ExternalBossBarText {
    fun measure(player: HudPlayer, component: Component): WidthComponent {
        val api = GlyphAPI.inst()
        fun font(c: Component): Component = c.font(api.defaultKey).children(c.children().map(::font))
        fun decorated(parent: Boolean, state: TextDecoration.State) = when (state) {
            TextDecoration.State.TRUE -> true; TextDecoration.State.FALSE -> false; TextDecoration.State.NOT_SET -> parent
        }
        fun width(c: Component, bold: Boolean, italic: Boolean): Int {
            val text = when (c) {
                is TextComponent -> c.content()
                is TranslatableComponent -> api.translate(player.locale().toLanguageTag(), c.key()) ?: c.key()
                else -> ""
            }
            var total = 0
            var offset = 0
            while (offset < text.length) {
                val point = text.codePointAt(offset); offset += Character.charCount(point)
                total += (if (point == 32) 4 else api.getWidth(point) + 1) + (if (bold) 1 else 0) + (if (italic) 1 else 0)
            }
            for (child in c.children()) total += width(child,
                decorated(bold, child.decoration(TextDecoration.BOLD)), decorated(italic, child.decoration(TextDecoration.ITALIC)))
            return total
        }
        return WidthComponent(Component.text().append(font(component)), width(component,
            decorated(false, component.decoration(TextDecoration.BOLD)), decorated(false, component.decoration(TextDecoration.ITALIC))))
    }
}
