package online.libang.glyph.renderer

import online.libang.glyph.api.state.HudState
import online.libang.glyph.api.state.HudValue
import kotlin.test.*

class RenderStateTest {
    @Test fun `one frame samples a fluctuating source once`() {
        var calls = 0
        val frame = FrameSamples()
        val result = frame.sample("fish") { ++calls }
        assertEquals(result, frame.sample("fish") { ++calls })
        assertEquals(1, calls)
        assertEquals(2, FrameSamples().sample("fish") { ++calls })
    }
    @Test fun `unrelated input changes do not render health segment`() {
        val state = HudState()
        var renders = 0
        val health = DependencyCache<Int>(listOf("health"))
        health.get(state.snapshot()) { ++renders }
        state.set("money", HudValue.Number(10.0))
        health.get(state.snapshot()) { ++renders }
        assertEquals(1, renders)
        state.set("health", HudValue.Number(19.0))
        health.get(state.snapshot()) { ++renders }
        assertEquals(2, renders)
        assertFalse(state.set("health", HudValue.Number(19.0)))
        health.get(state.snapshot()) { ++renders }
        assertEquals(2, renders)
        state.remove("health")
        health.get(state.snapshot()) { ++renders }
        assertEquals(3, renders)
    }
    @Test fun `atomic batch keeps old snapshot immutable`() {
        val state = HudState()
        val before = state.snapshot()
        state.setAll(mapOf("health" to HudValue.Number(10.0), "alive" to HudValue.Boolean(true)))
        assertTrue(before.values().isEmpty())
        val after = state.snapshot()
        assertEquals(after.version("health"), after.version("alive"))
        assertFailsWith<UnsupportedOperationException> { after.values()["x"] = HudValue.Text("x") }
    }
    @Test fun `failed render retries and force bypasses cache`() {
        val cache = DependencyCache<Int>(emptyList())
        val snapshot = HudState().snapshot()
        assertFails { cache.get(snapshot) { error("failed") } }
        assertEquals(1, cache.get(snapshot) { 1 })
        assertEquals(2, cache.get(snapshot, true) { 2 })
    }
}
