package com.github.m9w.customeventbroker

import eu.darkbot.api.events.Event

class CustomEvent(val event: String, val value: String) : Event {
    init { CustomEventBroker.sendEvent(this) }
}