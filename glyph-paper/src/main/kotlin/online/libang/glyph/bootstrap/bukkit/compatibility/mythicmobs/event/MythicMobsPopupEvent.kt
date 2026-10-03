package online.libang.glyph.bootstrap.bukkit.compatibility.mythicmobs.event

import io.lumine.mythic.api.adapters.AbstractPlayer
import io.lumine.mythic.api.skills.SkillCaster
import online.libang.glyph.api.bukkit.event.GlyphEvent
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class MythicMobsPopupEvent(
    val caster: SkillCaster,
    val target: AbstractPlayer
): Event(), GlyphEvent {
    companion object {
        @Suppress("UNUSED")
        fun getHandlerList(): HandlerList = GlyphEvent.HANDLER_LIST
    }
    override fun getHandlers(): HandlerList = GlyphEvent.HANDLER_LIST
}