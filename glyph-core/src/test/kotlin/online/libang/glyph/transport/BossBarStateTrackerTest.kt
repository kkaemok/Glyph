package online.libang.glyph.transport

import java.util.UUID
import kotlin.test.*

class BossBarStateTrackerTest {
    @Test fun `updates retain order and removal promotes latest complete state`() {
        val tracker = BossBarStateTracker<String>()
        val first = UUID.randomUUID(); val second = UUID.randomUUID()
        tracker.put(first, "first")
        tracker.put(second, "second")
        tracker.put(second, "updated name and style")
        assertEquals(listOf(first, second), tracker.entries().map { it.first })
        tracker.remove(first)
        assertEquals(second to "updated name and style", tracker.entries().single())
        tracker.clear()
        assertTrue(tracker.entries().isEmpty())
    }
}
