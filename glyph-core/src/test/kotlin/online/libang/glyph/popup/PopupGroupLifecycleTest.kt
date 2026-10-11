package online.libang.glyph.popup

import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.popup.*
import net.kyori.adventure.text.Component
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class PopupGroupLifecycleTest {
    private val groups = ConcurrentHashMap<String, PopupIteratorGroup>()

    @Test fun `render cannot see an empty group while an async popup is being published`() {
        val preparing = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val popup = TestIterator()
        popup.onIndex = {
            preparing.countDown()
            assertTrue(release.await(5, TimeUnit.SECONDS))
        }
        try {
            val show = pool.submit { PopupGroupLifecycle.add(groups, "dialogue", popup) }
            assertTrue(preparing.await(5, TimeUnit.SECONDS))
            assertTrue(groups.isEmpty(), "The group must be populated before it is published")
            assertTrue(PopupGroupLifecycle.render(groups).isEmpty())
            release.countDown()
            show.get(5, TimeUnit.SECONDS)
            assertEquals("dialogue", renderText())
            assertFalse(popup.markedAsRemoval())
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test fun `concurrent show attaches to a live group when rendering retires the previous one`() {
        val rendering = CountDownLatch(1)
        val release = CountDownLatch(1)
        val showing = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val old = TestIterator().apply {
            condition = {
                rendering.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
                false
            }
        }
        PopupGroupLifecycle.add(groups, "dialogue", old)
        val previous = groups["dialogue"]
        try {
            val frame = pool.submit<List<WidthComponent>> { PopupGroupLifecycle.render(groups) }
            assertTrue(rendering.await(5, TimeUnit.SECONDS))
            val fresh = TestIterator().apply { text = "new dialogue" }
            val show = pool.submit {
                showing.countDown()
                PopupGroupLifecycle.add(groups, "dialogue", fresh)
            }
            assertTrue(showing.await(5, TimeUnit.SECONDS))
            release.countDown()
            assertTrue(frame.get(5, TimeUnit.SECONDS).isEmpty())
            show.get(5, TimeUnit.SECONDS)
            assertNotSame(previous, groups["dialogue"])
            assertTrue(old.markedAsRemoval())
            assertEquals("new dialogue", renderText())
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test fun `empty frames keep active popup groups available for later updates`() {
        val popup = TestIterator().apply { emptyFrame = true }
        PopupGroupLifecycle.add(groups, "dialogue", popup)
        val group = groups["dialogue"]
        assertTrue(PopupGroupLifecycle.render(groups).isEmpty())
        assertSame(group, groups["dialogue"])
        popup.emptyFrame = false
        popup.text = "updated dialogue"
        assertEquals("updated dialogue", renderText())
    }

    @Test fun `removal retires only the empty group and a later dialogue can render`() {
        val popup = TestIterator()
        PopupGroupLifecycle.add(groups, "dialogue", popup)
        PopupGroupLifecycle.add(groups, "other", TestIterator().apply { text = "other" })
        popup.remove()
        assertEquals("other", renderText())
        assertNull(groups["dialogue"])
        PopupGroupLifecycle.add(groups, "dialogue", TestIterator().apply { text = "second" })
        val texts = PopupGroupLifecycle.render(groups).map { it.component().content() }.toSet()
        assertEquals(setOf("other", "second"), texts)
    }

    @Test fun `immediate dialogue replacement reuses a removed slot before the next render tick`() {
        val old = TestIterator()
        PopupGroupLifecycle.add(groups, "dialogue", old)
        old.remove()
        val fresh = TestIterator().apply { text = "next dialogue" }
        PopupGroupLifecycle.add(groups, "dialogue", fresh)
        assertEquals("next dialogue", renderText())
        assertEquals(1, groups["dialogue"]!!.index)
        assertFalse(fresh.markedAsRemoval())
    }

    @Test fun `removed unique dialogue does not block its immediate replacement`() {
        val old = TestIterator().apply { unique = true }
        PopupGroupLifecycle.add(groups, "dialogue", old)
        old.remove()
        PopupGroupLifecycle.add(groups, "dialogue", TestIterator().apply {
            unique = true
            text = "next unique dialogue"
        })
        assertEquals("next unique dialogue", renderText())
    }

    @Test fun `active unique dialogue still prevents duplicates`() {
        PopupGroupLifecycle.add(groups, "dialogue", TestIterator().apply { unique = true })
        PopupGroupLifecycle.add(groups, "dialogue", TestIterator().apply {
            unique = true
            text = "duplicate"
        })
        assertEquals("dialogue", renderText())
        assertEquals(1, groups["dialogue"]!!.index)
    }

    @Test fun `old updater removal cannot detach a replacement group`() {
        val old = TestIterator()
        PopupGroupLifecycle.add(groups, "dialogue", old)
        groups.remove("dialogue")!!.clear()
        PopupGroupLifecycle.add(groups, "dialogue", TestIterator().apply { text = "replacement" })
        old.remove()
        assertEquals("replacement", renderText())
    }

    private fun renderText() = PopupGroupLifecycle.render(groups).joinToString { it.component().content() }

    private class TestIterator : PopupIterator {
        private val uuid = UUID.randomUUID()
        private var position = -1
        private var removed = false
        var text = "dialogue"
        var unique = false
        var emptyFrame = false
        var onIndex: () -> Unit = {}
        var condition: () -> Boolean = { true }
        private val popup = Proxy.newProxyInstance(Popup::class.java.classLoader, arrayOf(Popup::class.java)) { _, method, _ ->
            when (method.name) {
                "getMaxStack" -> 1
                else -> error("Unexpected popup access: ${method.name}")
            }
        } as Popup

        override fun parent() = popup
        override fun isUnique() = unique
        override fun markedAsRemoval() = removed
        override fun push() = false
        override fun getIndex(): Int { onIndex(); return position }
        override fun setIndex(index: Int) { position = index }
        override fun getMaxIndex() = 0
        override fun available() = condition()
        override fun alwaysCheckCondition() = false
        override fun remove() { removed = true }
        override fun canSave() = false
        override fun getUUID() = uuid
        override fun getSortType() = PopupSortType.LAST
        override fun getKey(): Any = uuid
        override fun next() = if (emptyFrame) emptyList() else listOf(WidthComponent(Component.text().content(text), text.length))
        override fun name() = "dialogue"
        override fun getPriority() = -1
        override fun setPriority(priority: Int) = Unit
        override fun compareTo(other: PopupIterator) = position.compareTo(other.index)
    }
}
