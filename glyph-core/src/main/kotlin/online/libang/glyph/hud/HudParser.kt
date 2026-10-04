package online.libang.glyph.hud

import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.update.UpdateEvent
import online.libang.glyph.component.LayoutComponentContainer
import online.libang.glyph.location.PixelLocation
import online.libang.glyph.layout.LayoutGroup
import online.libang.glyph.location.GuiLocation
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.EMPTY_WIDTH_COMPONENT
import online.libang.glyph.util.Runner

class HudParser(
    hud: HudImpl,
    resource: GlobalResource,
    private val layout: LayoutGroup,
    gui: GuiLocation,
    pixel: PixelLocation
) {
    private val imageElement = layout.image.map { image ->
        HudImageParser(hud, image, gui, pixel)
    }
    private val textElement = layout.text.mapIndexed { index, textLayout ->
        HudTextParser(index + 1, hud, resource, textLayout, gui, pixel)
    }
    private val headElement = layout.head.map { image ->
        HudHeadParser(hud, image, gui, pixel)
    }

    private val elements = layout.order(
        imageElement,
        textElement,
        headElement
    )

    val conditions = layout.conditions build UpdateEvent.EMPTY

    private val max = imageElement.maxOfOrNull {
        it.max
    } ?: 0

    fun getComponent(player: HudPlayer): Runner<WidthComponent> {
        val cache = layout.dependencies?.let { online.libang.glyph.renderer.DependencyCache<WidthComponent>(it) }
        val renderer = elements.map {
            it.render(player)
        }
        val render = {
            if (conditions(player)) {
                val f = player.tick
                LayoutComponentContainer(layout.offset, layout.align, max, layout.flow)
                    .append(renderer.map {
                        it(f)
                    })
                    .build()
            } else EMPTY_WIDTH_COMPONENT
        }
        return Runner {
            cache?.get(online.libang.glyph.renderer.RenderFrame.state(player),
                online.libang.glyph.manager.ConfigManagerImpl.forceUpdate, render) ?: render()
        }
    }
}
