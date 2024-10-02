package com.github.m9w.launcher.gui.components

import java.util.function.Supplier
import javax.swing.JComboBox
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class JDropDown(provider: Supplier<Collection<String>>, default: String = "") : JComboBox<String>() {
    var onSelect: ((String) -> Unit)? = null

    init {
        apply(default)
        addPopupMenuListener (object  : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                val c = selectedItem
                removeAllItems()
                provider.get().forEach(::addItem)
                selectedItem = c
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                selectedItem?.toString()?.let { onSelect?.invoke(it) }
            }

            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    fun apply(block: String): String {
        removeAllItems()
        addItem(block)
        selectedItem = getItemAt(0)
        return block
    }

    fun onFocusLost(handler: (String) -> Unit) {
        onSelect = handler
    }

    fun getSelected(): String = selectedItem?.toString() ?: ""
}