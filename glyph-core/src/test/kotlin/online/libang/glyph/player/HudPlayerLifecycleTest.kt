package online.libang.glyph.player

import online.libang.glyph.api.adapter.LocationWrapper
import online.libang.glyph.api.adapter.WorldWrapper
import online.libang.glyph.api.scheduler.HudTask
import net.kyori.adventure.audience.Audience
import java.util.Locale
import java.util.UUID
import kotlin.test.*

class HudPlayerLifecycleTest {
    private class Owner {
        val id: UUID = UUID.randomUUID()
        val tasks = mutableListOf<TestTask>()
        fun schedule(): HudTask = TestTask().also(tasks::add)
    }

    private class TestTask : HudTask {
        private var cancelled = false
        override fun isCancelled() = cancelled
        override fun cancel() { cancelled = true }
    }

    // Kotlin assigns owner after HudPlayerImpl's constructor returns, just as
    // HudPlayerBukkit's Player field is assigned after its superclass returns.
    private class TestPlayer(private val owner: Owner) : HudPlayerImpl() {
        override fun uuid() = owner.id
        override fun name() = "test"
        override fun handle(): Any = owner
        override fun audience(): Audience = Audience.empty()
        override fun world() = WorldWrapper("world")
        override fun location() = LocationWrapper(world(), 0.0, 0.0, 0.0, 0f, 0f)
        override fun locale(): Locale = Locale.ROOT
        override fun hasPermission(perm: String) = true
        override fun updatePlaceholder() = Unit
        override fun scheduleOwned(period: Long, action: () -> Unit) = owner.schedule()
    }

    @Test fun `constructing a player does not access its uninitialized platform owner`() {
        val owner = Owner()
        val player = TestPlayer(owner)
        assertSame(owner, player.handle())
        assertTrue(owner.tasks.isEmpty())
    }

    @Test fun `tick starts explicitly after construction and can restart without leaking tasks`() {
        val owner = Owner()
        val player = TestPlayer(owner)
        player.cancelTick()
        assertTrue(owner.tasks.isEmpty())
        player.startTick()
        assertEquals(1, owner.tasks.size)
        assertFalse(owner.tasks[0].isCancelled())
        player.startTick()
        assertEquals(2, owner.tasks.size)
        assertTrue(owner.tasks[0].isCancelled())
        assertFalse(owner.tasks[1].isCancelled())
        player.cancelTick()
        assertTrue(owner.tasks[1].isCancelled())
    }
}
