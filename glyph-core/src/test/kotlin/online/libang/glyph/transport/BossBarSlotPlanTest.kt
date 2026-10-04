package online.libang.glyph.transport

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.UUID
import kotlin.test.*

/** Exercises the allocator and title routing called by both real transports, with complete bar state. */
class BossBarSlotPlanTest {
    private val hud = UUID.randomUUID()
    private val glyphFont = Key.key("glyph", "default")
    private val tracker = BossBarStateTracker<BossBar>()
    private fun bar(name: Component) = BossBar.bossBar(name, .75f, BossBar.Color.RED,
        BossBar.Overlay.NOTCHED_10, setOf(BossBar.Flag.PLAY_BOSS_MUSIC, BossBar.Flag.DARKEN_SCREEN))
    private fun plan(dummies: List<UUID> = emptyList()) = BossBarSlotPlan.allocate(tracker.entries(), dummies, hud) {
        ExternalBossBarText.canMerge(it.name(), glyphFont)
    }

    @Test fun `ordinary plugin bars merge in order and overflow remains standalone`() {
        val ids = List(3) { UUID.randomUUID() }
        ids.forEachIndexed { i, id -> tracker.put(id, bar(Component.text("Boss $i"))) }
        val dummy = UUID.randomUUID()
        val p = plan(listOf(dummy))
        assertEquals(mapOf(ids[0] to dummy, ids[1] to hud, ids[2] to ids[2]), p.assignments)
        assertEquals(ids[1], p.merged)
    }

    @Test fun `custom titles keep original font tree on standalone and dummy slots`() {
        val id = UUID.randomUUID()
        val original = ExternalBossBarTextTest.cnpTitle()
        tracker.put(id, bar(original))
        val json = GsonComponentSerializer.gson().serialize(original)
        for (dummies in listOf(emptyList(), listOf(UUID.randomUUID()))) {
            val p = plan(dummies)
            assertNull(p.merged)
            val output = p.title(id, tracker[id]!!.name(), Component.text("Glyph HUD"))
            assertSame(original, output)
            assertEquals(json, GsonComponentSerializer.gson().serialize(output))
            assertEquals(if (dummies.isEmpty()) id else dummies.single(), p.assignments[id])
        }
    }

    @Test fun `custom title does not block ordinary bars from merging`() {
        val custom = UUID.randomUUID(); val ordinary = UUID.randomUUID()
        tracker.put(custom, bar(ExternalBossBarTextTest.cnpTitle()))
        tracker.put(ordinary, bar(Component.text("Mythic boss")))
        assertEquals(mapOf(custom to custom, ordinary to hud), plan().assignments)
        assertEquals(ordinary, plan().merged)
    }

    @Test fun `update name changes eligibility and subsequent removal promotes full updated state`() {
        val first = UUID.randomUUID(); val second = UUID.randomUUID()
        tracker.put(first, bar(Component.text("first")))
        tracker.put(second, bar(Component.text("second")))
        assertEquals(first, plan().merged)
        val original = ExternalBossBarTextTest.cnpTitle()
        tracker[first]!!.name(original)
        assertEquals(mapOf(first to first, second to hud), plan().assignments)
        tracker[first]!!.name(Component.text("restored ordinary title"))
        assertEquals(first, plan().merged)
        tracker[second]!!.name(Component.text("latest"))
        tracker[second]!!.color(BossBar.Color.PURPLE).overlay(BossBar.Overlay.NOTCHED_20)
        tracker[second]!!.progress(.25f).addFlag(BossBar.Flag.CREATE_WORLD_FOG)
        tracker.remove(first)
        assertEquals(second, plan().merged)
        assertEquals("latest", (tracker[second]!!.name() as net.kyori.adventure.text.TextComponent).content())
        assertEquals(BossBar.Color.PURPLE, tracker[second]!!.color())
        assertEquals(BossBar.Overlay.NOTCHED_20, tracker[second]!!.overlay())
        assertEquals(.25f, tracker[second]!!.progress())
        assertEquals(BossBar.Flag.entries.toSet(), tracker[second]!!.flags())
    }

    @Test fun `style progress and properties updates preserve custom title and its routing`() {
        val id = UUID.randomUUID(); val original = ExternalBossBarTextTest.cnpTitle()
        tracker.put(id, bar(original))
        tracker[id]!!.color(BossBar.Color.BLUE).overlay(BossBar.Overlay.NOTCHED_6)
        tracker[id]!!.progress(.1f).removeFlag(BossBar.Flag.PLAY_BOSS_MUSIC)
        assertEquals(id, plan().assignments[id])
        assertSame(original, plan().title(id, tracker[id]!!.name(), Component.empty()))
        assertEquals(.1f, tracker[id]!!.progress())
        assertEquals(BossBar.Color.BLUE, tracker[id]!!.color())
        assertEquals(setOf(BossBar.Flag.DARKEN_SCREEN), tracker[id]!!.flags())
        tracker.remove(id)
        assertTrue(plan().assignments.isEmpty())
        assertNull(plan().merged)
    }

    @Test fun `dummy promotion retains custom text and ordinary HUD selection`() {
        val first = UUID.randomUUID(); val custom = UUID.randomUUID(); val ordinary = UUID.randomUUID()
        tracker.put(first, bar(Component.text("first")))
        val original = ExternalBossBarTextTest.cnpTitle()
        tracker.put(custom, bar(original)); tracker.put(ordinary, bar(Component.text("ordinary")))
        val dummy = UUID.randomUUID()
        assertEquals(mapOf(first to dummy, custom to custom, ordinary to hud), plan(listOf(dummy)).assignments)
        tracker.remove(first)
        val p = plan(listOf(dummy))
        assertEquals(mapOf(custom to dummy, ordinary to hud), p.assignments)
        assertSame(original, p.title(custom, tracker[custom]!!.name(), Component.empty()))
    }

    @Test fun `changing bossbar line reallocates every state without losing names or metadata`() {
        val custom = UUID.randomUUID(); val ordinary = UUID.randomUUID()
        val original = ExternalBossBarTextTest.cnpTitle()
        tracker.put(custom, bar(original)); tracker.put(ordinary, bar(Component.text("ordinary")))
        assertEquals(ordinary, plan().merged)
        val dummies = List(2) { UUID.randomUUID() }
        assertNull(plan(dummies).merged)
        assertEquals(mapOf(custom to dummies[0], ordinary to dummies[1]), plan(dummies).assignments)
        assertEquals(ordinary, plan().merged)
        assertSame(original, tracker[custom]!!.name())
        assertEquals(.75f, tracker[custom]!!.progress())
    }

    @Test fun `clearing tracker resets all external assignments and selection`() {
        tracker.put(UUID.randomUUID(), bar(ExternalBossBarTextTest.cnpTitle()))
        tracker.put(UUID.randomUUID(), bar(Component.text("ordinary")))
        tracker.clear()
        assertTrue(plan().assignments.isEmpty())
        assertNull(plan().merged)
    }
}
