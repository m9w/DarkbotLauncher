package com.github.m9w.launcher.gui.components

import com.github.m9w.launcher.launcher.ClientManager.sync
import java.util.function.Supplier
import javax.swing.JComboBox
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class JDropDownMulti(selectedDef: Collection<String>, provider: Supplier<Collection<String>>) :
    JComboBox<String>() {
    val selected = HashSet<String>()
    var onSelect: ((Set<String>) -> Unit)? = null

    constructor(values: Collection<String>, defaultValues: Collection<String> = emptySet()) : this(
        defaultValues,
        Supplier { values })

    init {
        apply(selectedDef)
        addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                selectedItem = null
                removeAllItems()
                addItem(null)
                provider.get().forEach {
                    addItem(if (selected.contains(it)) "██ $it" else "░░ $it")
                }
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                selectedItem?.toString()?.substring(3)?.let { if (!selected.remove(it)) selected.add(it) }
                draw()
                onSelect?.invoke(selected)
            }

            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    fun getSelected(): Set<String> = selected

    fun <T : Collection<String>> apply(block: T): T {
        selected.sync(block)
        draw()
        return block
    }

    fun onFocusLost(handler: (Set<String>) -> Unit) {
        onSelect = handler
    }

    private fun draw() {
        removeAllItems()
        var title = selected.joinToString(if (selected.size > 5) "," else ", ") { it.substring(0, 3) }
        if (title.length > 23) title = title.substring(0, 20) + "..."
        addItem(title)
        selectedItem = getItemAt(0)
    }
}
