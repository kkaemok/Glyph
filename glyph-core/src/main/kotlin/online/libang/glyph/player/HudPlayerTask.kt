package online.libang.glyph.player

import online.libang.glyph.api.scheduler.HudTask

class HudPlayerTask(
    private val creator: () -> HudTask?
) : HudTask {
    // Player task holders are created by the base constructor, before the
    // platform subclass has assigned its player field. Start explicitly later.
    @Volatile private var initialTask: HudTask? = null

    @Synchronized
    fun restart() {
        cancel()
        initialTask = creator()
    }

    @Synchronized
    override fun cancel() {
        val previous = initialTask
        initialTask = null
        previous?.cancel()
    }

    override fun isCancelled(): Boolean = initialTask?.isCancelled != false
}
