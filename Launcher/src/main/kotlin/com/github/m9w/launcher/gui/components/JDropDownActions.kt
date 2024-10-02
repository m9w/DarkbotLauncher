package com.github.m9w.launcher.gui.components

import java.util.function.Consumer
import javax.swing.JComboBox
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class JDropDownActions(handler: Consumer<Actions>) : JComboBox<JDropDownActions.Actions>() {
    init {
        addItem(Actions.Skip)
        selectedItem = Actions.Skip
        addPopupMenuListener (object  : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                removeAllItems()
                addItem(null)
                Actions.entries.stream().skip(1).forEach { addItem(it) }
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                try {
                    handler.accept(selectedItem as Actions)
                } catch (e: Exception) {e.printStackTrace()}

                removeAllItems()
                addItem(Actions.Skip)
                selectedItem = Actions.Skip
            }

            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        })
    }

    enum class Actions(private val title: String) {
        Skip("Actions"),
        Start("Start"),
        Stop("Stop"),
        Show("Show"),
        Hide("Hide"),
        ExtractConfig("Get conf"),
        Clone("Clone"),
        Delete("Drop");

        override fun toString(): String = title
    }
}
