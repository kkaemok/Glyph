package online.libang.glyph.renderer

import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import online.libang.glyph.api.state.HudState
import online.libang.glyph.api.state.HudValue
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.api.update.PopupUpdateEvent
import kr.toxicity.hud.api.popup.PopupIterator
import online.libang.glyph.manager.PlaceholderManagerImpl
import online.libang.glyph.placeholder.PlaceholderSource
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.function.Function
import kotlin.test.*

class PlaceholderFrameTest {
    private fun popup(index: Int): PopupIterator = Proxy.newProxyInstance(
        PopupIterator::class.java.classLoader, arrayOf(PopupIterator::class.java)
    ) { _, method, _ ->
        when (method.name) { "getIndex" -> index; else -> error("Unexpected iterator access: ${method.name}") }
    } as PopupIterator

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

    @Test fun `popups sharing a trigger key keep their own iterator samples`() {
        val player = player(HudState())
        val firstEvent = PopupUpdateEvent(UpdateEvent.EMPTY, popup(0))
        val secondEvent = PopupUpdateEvent(UpdateEvent.EMPTY, popup(1))
        assertSame(firstEvent.key, secondEvent.key)
        val builder = PlaceholderManagerImpl.find("popup_index", PlaceholderSource.empty)
        val first = builder.build(firstEvent)
        val second = builder.build(secondEvent)
        RenderFrame.render {
            assertEquals(0.0, first.value(player))
            assertEquals(1.0, second.value(player))
            assertEquals(0.0, first.value(player))
        }
    }

    @Test fun `equal event objects with different payloads cannot share samples`() {
        class EqualEvent(val payload: Int) : UpdateEvent {
            override fun getType() = UpdateEvent.EMPTY.type
            override fun getKey() = UpdateEvent.EMPTY.key
            override fun equals(other: Any?) = other is EqualEvent
            override fun hashCode() = 1
        }
        var calls = 0
        val source = HudPlaceholder.of<Number> { _, event ->
            Function { ++calls; (event as EqualEvent).payload }
        }
        PlaceholderManagerImpl.numberContainer.addPlaceholder("test_context", source)
        val builder = PlaceholderManagerImpl.find("test_context", PlaceholderSource.empty)
        val first = builder.build(EqualEvent(10))
        val second = builder.build(EqualEvent(20))
        val player = player(HudState())
        RenderFrame.render {
            assertEquals(10.0, first.value(player))
            assertEquals(20.0, second.value(player))
            assertEquals(10.0, first.value(player))
            assertEquals(2, calls)
        }
    }
}
