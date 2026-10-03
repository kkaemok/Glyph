package online.libang.glyph.bootstrap.velocity.manager

import online.libang.glyph.bootstrap.velocity.module.MODULE_VELOCITY
import online.libang.glyph.manager.ListenerManagerImpl
import online.libang.glyph.manager.PlaceholderManagerImpl
import online.libang.glyph.manager.TriggerManagerImpl
import online.libang.glyph.util.handleFailure
import java.util.function.Function

object ModuleManager {
    fun start() {
        MODULE_VELOCITY.forEach { module ->
            runCatching {
                val value = module.value()
                fun String.key(tag: String) = if (this == "standard") tag else "${this}_$tag"
                value.triggers.forEach { trigger ->
                    TriggerManagerImpl.addTrigger(module.key.key(trigger.key), trigger.value)
                }
                value.listeners.forEach { listener ->
                    ListenerManagerImpl.addListener(module.key.key(listener.key)) { c ->
                        val original = listener.value(c)
                        Function { f ->
                            original(f)
                        }
                    }
                }
                value.strings.forEach { string ->
                    PlaceholderManagerImpl.stringContainer.addPlaceholder(module.key.key(string.key), string.value)
                }
                value.numbers.forEach { number ->
                    PlaceholderManagerImpl.numberContainer.addPlaceholder(module.key.key(number.key), number.value)
                }
                value.booleans.forEach { boolean ->
                    PlaceholderManagerImpl.booleanContainer.addPlaceholder(module.key.key(boolean.key), boolean.value)
                }
            }.handleFailure {
                "Unable to load this module: ${module.key}"
            }
        }
    }

}