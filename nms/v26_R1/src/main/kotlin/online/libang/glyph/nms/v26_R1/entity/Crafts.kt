package online.libang.glyph.nms.v26_R1.entity

import online.libang.glyph.api.GlyphAPI
import net.minecraft.world.entity.Entity
import org.bukkit.craftbukkit.entity.CraftEntity

val IS_PAPER by lazy {
    GlyphAPI.inst().bootstrap().isPaper
}

inline fun <reified T, reified R> createAdaptedFieldGetter(noinline paperGetter: (T) -> R): (T) -> R {
    return if (IS_PAPER) paperGetter else T::class.java.declaredFields.first {
        R::class.java.isAssignableFrom(it.type)
    }.apply {
        isAccessible = true
    }.let { getter ->
        { t ->
            getter[t] as R
        }
    }
}

val CraftEntity.unsafeHandle: Entity
    get() = if (IS_PAPER) handleRaw else handle