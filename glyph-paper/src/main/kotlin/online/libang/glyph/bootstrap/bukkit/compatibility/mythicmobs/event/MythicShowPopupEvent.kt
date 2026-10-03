package online.libang.glyph.bootstrap.bukkit.compatibility.mythicmobs.event

import io.lumine.mythic.api.adapters.AbstractPlayer
import io.lumine.mythic.api.skills.SkillCaster
import online.libang.glyph.api.bukkit.event.GlyphEvent
import org.bukkit.event.HandlerList

class MythicShowPopupEvent(caster: SkillCaster, target: AbstractPlayer): MythicMobsPopupEvent(caster, target) {
    companion object {
        @Suppress("UNUSED")
        fun getHandlerList(): HandlerList = GlyphEvent.HANDLER_LIST
    }
}