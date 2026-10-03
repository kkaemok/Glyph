package online.libang.glyph.pack

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import online.libang.glyph.util.jsonArrayOf
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.io.Reader
import kotlin.math.max

data class PackMeta(
    val pack: Pack,
    val overlays: Overlay? = Overlay()
) {
    companion object {
        private val gson = GsonBuilder()
            .registerTypeAdapter(VersionFormat::class.java, JsonDeserializer<VersionFormat> { src, _, _ ->
                when {
                    src.isJsonPrimitive -> VersionFormat(src.asInt)
                    src.isJsonArray -> src.asJsonArray.run {
                        if (size() < 2) VersionFormat(get(0).asInt) else VersionFormat(get(0).asInt, get(1).asInt)
                    }
                    else -> null
                }
            })
            .registerTypeAdapter(VersionFormat::class.java, JsonSerializer<VersionFormat> { src, _, _ -> src.asJson() })
            .registerTypeAdapter(VersionRange::class.java, JsonDeserializer<VersionRange> { src, _, _ ->
                when (src) {
                    is JsonObject -> VersionRange(
                        src.getAsJsonPrimitive("min_inclusive").asInt,
                        src.getAsJsonPrimitive("max_inclusive").asInt
                    )
                    is JsonPrimitive -> {
                        src.asInt.let {
                            VersionRange(it, it)
                        }
                    }
                    is JsonArray -> VersionRange(
                        src.get(0).asInt,
                        src.get(1).asInt
                    )
                    else -> null
                }
            })
            .registerTypeAdapter(VersionRange::class.java, JsonSerializer<VersionRange> { src, _, _ -> src.asJson() })
            .create()

        val default by lazy {
            val maxFormat = PackOverlay.entries.maxOf { it.maxVersion }
            PackMeta(
                Pack(
                    maxFormat,
                    JsonPrimitive("Glyph's default resource pack."),
                    VersionRange(84, maxFormat),
                    VersionFormat(84),
                    VersionFormat(maxFormat, Int.MAX_VALUE)
                ),
                Overlay(PackOverlay.entries.map {
                    OverlayEntry(
                        VersionRange(it.minVersion, it.maxVersion),
                        it.overlayName
                    )
                })
            )
        }

        fun from(array: ByteArray): PackMeta = InputStreamReader(ByteArrayInputStream(array), Charsets.UTF_8).use(::read)
        fun from(file: File): PackMeta = file.bufferedReader().use(::read)

        private fun read(reader: Reader): PackMeta {
            val json = JsonParser.parseReader(reader).asJsonObject
            normalizeBounds(json.getAsJsonObject("pack"), false)
            json.getAsJsonObject("overlays")?.getAsJsonArray("entries")?.forEach {
                normalizeBounds(it.asJsonObject, true)
            }
            return gson.fromJson(json, PackMeta::class.java)
        }

        private fun normalizeBounds(section: JsonObject, overlay: Boolean) {
            // Minecraft's integer maximum includes every minor version, whereas
            // an explicit [major, 0] excludes .1. Preserve that distinction.
            for ((key, defaultMinor) in listOf("min_format" to 0, "max_format" to Int.MAX_VALUE)) {
                val value = section[key] ?: continue
                if (value.isJsonPrimitive || value.isJsonArray && value.asJsonArray.size() == 1) {
                    val major = if (value.isJsonArray) value.asJsonArray[0].asInt else value.asInt
                    section.add(key, jsonArrayOf(major, defaultMinor))
                }
            }
            if (overlay && !section.has("formats") && section.has("min_format") && section.has("max_format")) {
                section.add("formats", VersionRange(section["min_format"].asJsonArray[0].asInt,
                    section["max_format"].asJsonArray[0].asInt).asJson())
            }
            if (!overlay && !section.has("pack_format") && section.has("max_format")) {
                section.addProperty("pack_format", section["max_format"].asJsonArray[0].asInt)
            }
        }
    }

    operator fun plus(other: PackMeta): PackMeta {
        val o1 = overlays
        val o2 = other.overlays
        return PackMeta(
            pack + other.pack,
            when {
                o1 != null && o2 != null -> o1 + o2
                o1 != null -> o1
                o2 != null -> o2
                else -> null
            }
        )
    }

    fun toByteArray(): ByteArray {
        val json = gson.toJsonTree(this).asJsonObject
        if ((pack.minFormat?.major ?: pack.supportedFormats?.min ?: pack.packFormat) >= 65) {
            json.getAsJsonObject("pack").apply { remove("pack_format"); remove("supported_formats") }
        }
        // Legacy formats are required for every entry only when an overlay
        // reaches a pre-minor resource format. Our 26.x-only output omits them.
        if (overlays?.entries.orEmpty().all { (it.minFormat?.major ?: it.formats.min) >= 65 }) {
            json.getAsJsonObject("overlays")?.getAsJsonArray("entries")?.forEach { it.asJsonObject.remove("formats") }
        }
        return gson.toJson(json).toByteArray(Charsets.UTF_8)
    }

    data class Pack(
        @SerializedName("pack_format") val packFormat: Int,
        val description: JsonElement,
        @SerializedName("supported_formats") val supportedFormats: VersionRange?,
        @SerializedName("min_format") val minFormat: VersionFormat?,
        @SerializedName("max_format") val maxFormat: VersionFormat?
    ) {
        operator fun plus(other: Pack): Pack {
            val lower = lowerBound() max other.lowerBound()
            val upper = upperBound() min other.upperBound()
            require(lower <= upper) { "Resource pack format ranges do not overlap: $lower .. $upper" }
            return Pack(
                max(packFormat, other.packFormat),
                other.description,
                VersionRange(lower.major, upper.major),
                lower,
                upper
            )
        }

        private fun lowerBound() = minFormat ?: VersionFormat(supportedFormats?.min ?: packFormat)
        private fun upperBound() = maxFormat ?: VersionFormat(supportedFormats?.max ?: packFormat, Int.MAX_VALUE)
    }

    data class Overlay(
        val entries: List<OverlayEntry> = emptyList()
    ) {
        operator fun plus(other: Overlay): Overlay {
            return Overlay((entries + other.entries)
                .asSequence()
                .sorted()
                .distinctBy {
                    it.directory
                }
                .toList())
        }
    }

    data class OverlayEntry(
        val formats: VersionRange,
        val directory: String,
        @SerializedName("min_format") val minFormat: VersionFormat?,
        @SerializedName("max_format") val maxFormat: VersionFormat?
    ) : Comparable<OverlayEntry> {

        constructor(
            formats: VersionRange,
            directory: String
        ) : this (
            formats,
            directory,
            VersionFormat(formats.min),
            VersionFormat(formats.max, Int.MAX_VALUE)
        )

        fun appliesTo(format: VersionFormat): Boolean =
            format >= (minFormat ?: VersionFormat(formats.min)) &&
                format <= (maxFormat ?: VersionFormat(formats.max, Int.MAX_VALUE))

        override fun compareTo(other: OverlayEntry): Int {
            return formats.compareTo(other.formats)
        }
    }

    data class VersionFormat( //1.21.9
        val major: Int,
        val minor: Int
    ): Comparable<VersionFormat> {
        private companion object {
            val comparator: Comparator<VersionFormat> = compareBy<VersionFormat> {
                it.major
            }.thenComparing {
                it.minor
            }
        }

        constructor(major: Int) : this(major, 0)

        // Always use the full form so external mergers cannot narrow an
        // integer upper bound to major.0 when reserializing pack metadata.
        fun asJson() = jsonArrayOf(
            major,
            minor
        )

        infix fun min(other: VersionFormat) = if (this < other) this else other
        infix fun max(other: VersionFormat) = if (this < other) other else this

        override fun compareTo(other: VersionFormat): Int = comparator.compare(this, other)
    }

    data class VersionRange(
        val min: Int,
        val max: Int
    ) : Comparable<VersionRange> {
        infix fun merge(other: VersionRange) = VersionRange(
            min.coerceAtLeast(other.min),
            max.coerceAtMost(other.max)
        )

        override fun compareTo(other: VersionRange): Int {
            return max.compareTo(other.max)
        }

        fun asJson() = if (min == max) JsonPrimitive(min) else jsonArrayOf(
            min,
            max
        )
    }
}
