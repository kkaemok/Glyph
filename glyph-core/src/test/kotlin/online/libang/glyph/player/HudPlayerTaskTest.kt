package online.libang.glyph.player

import online.libang.glyph.api.scheduler.HudTask
import kotlin.test.*

class HudPlayerTaskTest {
    @Test fun `failed restart cancels the old task and leaves the holder stopped`() {
        var cancelled = false
        var fail = false
        val first = object : HudTask {
            override fun isCancelled() = cancelled
            override fun cancel() { cancelled = true }
        }
        val task = HudPlayerTask {
            if (fail) error("scheduler unavailable")
            first
        }
        assertTrue(task.isCancelled())
        task.restart()
        fail = true
        assertFailsWith<IllegalStateException> { task.restart() }
        assertTrue(cancelled)
        assertTrue(task.isCancelled())
    }
}
