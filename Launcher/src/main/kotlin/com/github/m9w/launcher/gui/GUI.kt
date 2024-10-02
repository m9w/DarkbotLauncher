package com.github.m9w.launcher.gui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.ui.FlatNativeWindowBorder
import com.formdev.flatlaf.util.SystemInfo
import java.awt.Color
import java.awt.Image
import javax.swing.ImageIcon
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.UIManager


object GUI {
    val ICON: Image = ImageIcon(GUI.javaClass.getResource("/icon.png")).image

    init {
        UIManager.put("MenuItem.selectionType", "underline")
        UIManager.getFont("Label.font")
        UIManager.put("TitlePane.noIconLeftGap", 0)
        UIManager.put("OptionPane.showIcon", true)
        JFrame.setDefaultLookAndFeelDecorated(true)
        JDialog.setDefaultLookAndFeelDecorated(true)
        FlatNativeWindowBorder.isSupported()
        UIManager.setLookAndFeel(DarkLaf)
        UIManager.put("Button.arc", 0)
        UIManager.put("Component.arc", 0)
        UIManager.put("Button.default.boldText", false)
        UIManager.put("Table.cellFocusColor", Color(0, 0, 0, 160))

        Launcher
    }

    private object DarkLaf : FlatDarkLaf() {
        private fun readResolve(): Any = DarkLaf
        override fun getSupportsWindowDecorations(): Boolean {
            if (SystemInfo.isProjector || SystemInfo.isWebswing || SystemInfo.isWinPE) return false
            return !(SystemInfo.isWindows_10_orLater && FlatNativeWindowBorder.isSupported())
        }
    }
}
