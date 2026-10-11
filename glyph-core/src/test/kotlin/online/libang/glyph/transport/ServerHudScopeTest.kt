package online.libang.glyph.transport

import kotlin.test.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ServerHudScopeTest {
    private val calls = mutableListOf<String>()
    private val scope = ServerHudScope<String>(
        { calls += "enter:$it" }, { player, restore -> calls += "leave:$player:$restore" })

    @Test fun `default empty scope never creates proxy HUDs on any backend`() {
        scope.reconcile("player", null, emptySet())
        scope.reconcile("player", "paper-with-glyph", emptySet())
        scope.reconcile("player", "vanilla", emptySet())
        assertTrue(calls.isEmpty())
        assertFalse(scope.contains("player", "vanilla"))
    }

    @Test fun `login waits for an opted in backend and repeated events do not duplicate HUDs`() {
        scope.reconcile("player", null, setOf("lobby"))
        scope.reconcile("player", "survival", setOf("lobby"))
        assertTrue(calls.isEmpty())
        scope.reconcile("player", "lobby", setOf("lobby"))
        scope.reconcile("player", "lobby", setOf("lobby"))
        assertEquals(listOf("enter:player"), calls)
        assertTrue(scope.contains("player", "lobby"))
        assertFalse(scope.contains("player", "survival"))
    }

    @Test fun `switching to server without Glyph tears down HUD without resurrecting old bossbars`() {
        scope.reconcile("player", "lobby", setOf("lobby"))
        scope.reconcile("player", "vanilla", setOf("lobby"))
        assertEquals(listOf("enter:player", "leave:player:false"), calls)
        assertFalse(scope.contains("player", "lobby"))
        assertFalse(scope.contains("player", "vanilla"))
        scope.reconcile("player", "lobby", setOf("lobby"))
        assertEquals("enter:player", calls.last())
    }

    @Test fun `two allowed backends get independent transports and external state`() {
        val allowed = setOf("lobby", "survival")
        scope.reconcile("player", "lobby", allowed)
        scope.reconcile("player", "survival", allowed)
        assertEquals(listOf("enter:player", "leave:player:false", "enter:player"), calls)
        assertFalse(scope.contains("player", "lobby"))
        assertTrue(scope.contains("player", "survival"))
    }

    @Test fun `configuration removal restores current backend bars and stops proxy ownership`() {
        scope.reconcile("player", "lobby", setOf("lobby"))
        scope.reconcile("player", "lobby", emptySet())
        assertEquals(listOf("enter:player", "leave:player:true"), calls)
        assertFalse(scope.contains("player", "lobby"))
    }

    @Test fun `disconnect clears state once and does not restore obsolete bars`() {
        scope.reconcile("player", "lobby", setOf("lobby"))
        scope.disconnect("player")
        scope.disconnect("player")
        assertEquals(listOf("enter:player", "leave:player:false"), calls)
        assertFalse(scope.contains("player", "lobby"))
    }

    @Test fun `reload cleanup restores live external bars and can enable only allowed players again`() {
        scope.reconcile("one", "lobby", setOf("*"))
        scope.reconcile("two", "vanilla", setOf("*"))
        calls.clear()
        scope.clear(true)
        assertEquals(setOf("leave:one:true", "leave:two:true"), calls.toSet())
        assertFalse(scope.contains("one", "lobby"))
        calls.clear()
        scope.reconcile("one", "lobby", setOf("lobby"))
        scope.reconcile("two", "vanilla", setOf("lobby"))
        assertEquals(listOf("enter:one"), calls)
    }

    @Test fun `wildcard is explicit and never activates before a backend connection`() {
        scope.reconcile("player", null, setOf("*"))
        assertTrue(calls.isEmpty())
        scope.reconcile("player", "any-backend", setOf("*"))
        assertTrue(scope.contains("player", "any-backend"))
    }

    @Test fun `failed transport creation removes ownership and cleans up partial creation`() {
        val failing = ServerHudScope<String>({ error("transport failed") },
            { player, restore -> calls += "leave:$player:$restore" })
        assertFailsWith<IllegalStateException> { failing.reconcile("player", "lobby", setOf("lobby")) }
        assertFalse(failing.contains("player", "lobby"))
        assertEquals(listOf("leave:player:true"), calls)
    }

    @Test fun `queued asynchronous connection event reads latest backend after prior transition completes`() {
        val entering = CountDownLatch(1)
        val release = CountDownLatch(1)
        val queued = CountDownLatch(1)
        val liveServer = AtomicReference("lobby")
        val pool = Executors.newFixedThreadPool(2)
        val concurrent = ServerHudScope<String>({
            entering.countDown()
            check(release.await(5, TimeUnit.SECONDS))
        }, { _, _ -> })
        try {
            val first = pool.submit { concurrent.reconcile("player", "lobby", setOf("lobby")) }
            assertTrue(entering.await(5, TimeUnit.SECONDS))
            val next = pool.submit {
                queued.countDown()
                concurrent.reconcile("player", { liveServer.get() }, { setOf("lobby", "survival") })
            }
            assertTrue(queued.await(5, TimeUnit.SECONDS))
            liveServer.set("vanilla")
            release.countDown()
            first.get(5, TimeUnit.SECONDS)
            next.get(5, TimeUnit.SECONDS)
            assertFalse(concurrent.contains("player", "lobby"))
            assertFalse(concurrent.contains("player", "vanilla"))
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }
}
