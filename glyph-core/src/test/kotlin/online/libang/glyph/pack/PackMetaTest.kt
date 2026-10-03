package online.libang.glyph.pack

import com.google.gson.JsonParser
import kotlin.test.*

class PackMetaTest {
    @Test fun `explicit upper bounds survive mergers and activate 26_3 minor one`() {
        val meta = PackMeta.default
        val json = JsonParser.parseString(meta.toByteArray().toString(Charsets.UTF_8)).asJsonObject
        assertEquals(Int.MAX_VALUE, json["pack"].asJsonObject["max_format"].asJsonArray[1].asInt)
        for (entry in json["overlays"].asJsonObject["entries"].asJsonArray) {
            assertEquals(Int.MAX_VALUE, entry.asJsonObject["max_format"].asJsonArray[1].asInt)
        }
        val modern = meta.overlays!!.entries.single { it.directory == "glyph_26_3" }
        assertTrue(modern.appliesTo(PackMeta.VersionFormat(97, 1)))
        assertFalse(modern.appliesTo(PackMeta.VersionFormat(98)))
    }

    @Test fun `CraftEngine modern metadata preserves component descriptions and exact minor bounds`() {
        val fixture = """{"pack":{"description":{"color":"gray","text":"CraftEngine ResourcePack"},"min_format":[97,1],"max_format":[97,1]},"overlays":{"entries":[{"directory":"glyph_26_3","min_format":[97,0],"max_format":[97,0]}]}}"""
        val parsed = PackMeta.from(fixture.toByteArray())
        assertEquals("CraftEngine ResourcePack", parsed.pack.description.asJsonObject["text"].asString)
        val overlay = parsed.overlays!!.entries.single()
        assertEquals(PackMeta.VersionRange(97, 97), overlay.formats)
        assertTrue(overlay.appliesTo(PackMeta.VersionFormat(97)))
        assertFalse(overlay.appliesTo(PackMeta.VersionFormat(97, 1)))
        assertEquals(parsed, PackMeta.from(parsed.toByteArray()))
    }

    @Test fun `integer and one element maximums include every minor while two element zero stays exact`() {
        for (upper in listOf("97", "[97]")) {
            val parsed = PackMeta.from("""{"pack":{"description":"test","min_format":84,"max_format":$upper},"overlays":{"entries":[{"directory":"modern","min_format":97,"max_format":$upper}]}}""".toByteArray())
            assertEquals(Int.MAX_VALUE, parsed.pack.maxFormat!!.minor)
            assertTrue(parsed.overlays!!.entries.single().appliesTo(PackMeta.VersionFormat(97, 1)))
            assertEquals(parsed, PackMeta.from(parsed.toByteArray()))
        }
        val exact = PackMeta.from("""{"pack":{"description":"test","min_format":97,"max_format":[97,0]}}""".toByteArray())
        assertEquals(0, exact.pack.maxFormat!!.minor)
    }

    @Test fun `merging with a 97_1 pack retains a valid intersection and active shader overlay`() {
        val other = PackMeta.from("""{"pack":{"description":"CraftEngine","min_format":[97,1],"max_format":[97,1]}}""".toByteArray())
        val merged = PackMeta.from((PackMeta.default + other).toByteArray())
        assertEquals(PackMeta.VersionFormat(97, 1), merged.pack.minFormat)
        assertEquals(PackMeta.VersionFormat(97, 1), merged.pack.maxFormat)
        assertTrue(merged.overlays!!.entries.single { it.directory == "glyph_26_3" }.appliesTo(PackMeta.VersionFormat(97, 1)))
    }

    @Test fun `26x metadata omits legacy format fields and incompatible merges fail`() {
        val json = JsonParser.parseString(PackMeta.default.toByteArray().toString(Charsets.UTF_8)).asJsonObject
        assertFalse(json["pack"].asJsonObject.has("pack_format"))
        assertFalse(json["pack"].asJsonObject.has("supported_formats"))
        assertTrue(json["overlays"].asJsonObject["entries"].asJsonArray.all { !it.asJsonObject.has("formats") })
        val future = PackMeta.from("""{"pack":{"description":"future","min_format":98,"max_format":98}}""".toByteArray())
        assertFailsWith<IllegalArgumentException> { PackMeta.default + future }
    }
}
