package online.libang.glyph.renderer

import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.placeholder.HudPlaceholder
import online.libang.glyph.api.state.HudState
import online.libang.glyph.api.state.HudValue
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.manager.PlaceholderManagerImpl
import online.libang.glyph.placeholder.PlaceholderSource
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.function.Function
import kotlin.test.*

class PlaceholderFrameTest {
    private fun player(state: HudState): HudPlayer {
        val uuid = UUID.randomUUID()
        return Proxy.newProxyInstance(HudPlayer::class.java.classLoader, arrayOf(HudPlayer::class.java)) { _, method, _ ->
            when (method.name) { "uuid" -> uuid; "getHudState" -> state; else -> error("Unexpected player access: ${method.name}") }
        } as HudPlayer
    }
    @Test fun `different condition consumers receive the same fluctuating source sample`() {
        var calls = 0
        val container = PlaceholderManagerImpl.numberContainer
        container.addPlaceholder("test_fluctuating", HudPlaceholder.of { _, _ -> Function<HudPlayer, Number> { ++calls } })
        val a = PlaceholderManagerImpl.find("test_fluctuating", PlaceholderSource.empty).build(UpdateEvent.EMPTY)
        val b = PlaceholderManagerImpl.find("test_fluctuating", PlaceholderSource.empty).build(UpdateEvent.EMPTY)
        val player = player(HudState())
        RenderFrame.render {
            assertEquals(a.value(player), b.value(player))
            assertEquals(1, calls)
        }
        RenderFrame.render { assertEquals(2.0, a.value(player)) }
    }
    @Test fun `native placeholders see one immutable batch during a frame`() {
        val state = HudState()
        state.set("health", HudValue.Number(10.0))
        val player = player(state)
        val value = PlaceholderManagerImpl.find("state_number:health", PlaceholderSource.empty).build(UpdateEvent.EMPTY)
        RenderFrame.render {
            assertEquals(10.0, value.value(player))
            state.set("health", HudValue.Number(9.0))
            assertEquals(10.0, value.value(player))
        }
        RenderFrame.render { assertEquals(9.0, value.value(player)) }
    }
}
