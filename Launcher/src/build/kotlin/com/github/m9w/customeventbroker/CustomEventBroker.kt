package com.github.m9w.customeventbroker

import eu.darkbot.api.events.Listener
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.reflect.KCallable
import kotlin.reflect.full.findAnnotation

object CustomEventBroker {
    private val dispatchers = WeakHashMap<Listener, List<Consumer<CustomEvent>>>()
    private val toAdd: MutableSet<Listener> = Collections.newSetFromMap(WeakHashMap())
    private val toRemove: MutableSet<Listener> = Collections.newSetFromMap(WeakHashMap())
    private var eventsBeingSent = 0

    @Synchronized
    fun sendEvent(event: CustomEvent) {
        var thr: Throwable? = null
        eventsBeingSent++
        try {
            dispatchers.forEach { (_, d) -> d.forEach { it.accept(event) } }
        } catch (t: Throwable) {
            thr = t
        }
        eventsBeingSent--
        if (eventsBeingSent == 0) {
            toRemove.forEach { key: Listener -> dispatchers.remove(key) }
            toRemove.clear()
            toAdd.forEach { listener: Listener -> this.registerListener(listener) }
            toAdd.clear()
        }
        if (thr != null) throw thr
    }

    @Synchronized
    fun registerListener(listener: Listener) {
        if (dispatchers.containsKey(listener)) unregisterListener(listener)
        if (eventsBeingSent == 0) listener::class.members.mapNotNull { prepare(listener, it) }
                                    .let { if(it.isNotEmpty()) dispatchers[listener] = it }
        else toAdd.add(listener)
    }

    @Synchronized
    fun unregisterListener(listener: Listener) {
        if (eventsBeingSent == 0) {
            dispatchers.remove(listener)
        } else {
            toRemove.add(listener)
        }
    }

    private fun prepare(listener: Listener, callable: KCallable<*>): Consumer<CustomEvent>? {
        val annotation = callable.findAnnotation<CustomEventHandler>() ?: return null
        val pattern = Pattern.compile(annotation.regEx)
        val value = annotation.value

        val param = callable.parameters
        return if (param.isEmpty() || param[0].type.classifier != listener::class)
            null
        else if (param.size == 1)
            Consumer { if (shouldPerform(it, pattern, value)) callable.call(listener) }
        else if (param.size == 2 && param[1].type.classifier == CustomEvent::class)
            Consumer { if (shouldPerform(it, pattern, value)) callable.call(listener, it) }
        else if (param.size == 2 && param[1].type.classifier == String::class)
            Consumer { if (shouldPerform(it, pattern, value)) callable.call(listener, it.value) }
        else if (param.size == 3 && param[1].type.classifier == String::class && param[2].type.classifier == String::class)
            Consumer { if (shouldPerform(it, pattern, value)) callable.call(listener, it.value, it.event) }
        else throw RuntimeException("Listener ${listener::class.simpleName}#${callable.name} has unexpected signature. Allowed signatures: (Unit), (CustomEvent), (String), (String, String)")
    }

    private fun shouldPerform(event: CustomEvent, pattern: Pattern, value: String): Boolean {
        if (value.isEmpty()) return pattern.matcher(event.event).matches()
        return value == event.event
    }
}
