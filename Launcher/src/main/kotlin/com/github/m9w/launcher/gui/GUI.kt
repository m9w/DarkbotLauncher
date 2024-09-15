package com.github.m9w.launcher.gui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.ui.FlatNativeWindowBorder
import com.formdev.flatlaf.util.SystemInfo
import com.github.m9w.launcher.builder.BuildManager
import com.github.m9w.launcher.patches.Patch
import com.github.m9w.launcher.patches.PatchManager
import com.github.m9w.launcher.properties.LauncherProperties
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.*


object GUI {
    private var currentPatchList: List<Patch> = emptyList()

    init {
        UIManager.put("MenuItem.selectionType", "underline")
        UIManager.getFont("Label.font") // Prevents a linux crash
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

        val frame = JFrame("Darkbot launcher builder")
        frame.pack()
        frame.setSize(350, 600)
        frame.setLocationRelativeTo(null)
        frame.layout = BorderLayout()

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

        val patchList = JPanel()
        patchList.layout = BoxLayout(patchList, BoxLayout.Y_AXIS)

        val scrollPane = JScrollPane(patchList)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.preferredSize = Dimension(350, 200)
        frame.add(scrollPane, BorderLayout.CENTER)

        updatePatchesList(patchList)

        frame.add(JButton("Store changes as patch").apply {
            addActionListener {
                try {
                    val patchName = JOptionPane.showInputDialog(parent, "Enter patch name: ", null)
                    PatchManager.storeChangesAsPatch(patchName)
                } catch (e: Exception) {
                    println("Input interrupted")
                }
                patchList.removeAll()
                updatePatchesList(patchList)
                patchList.revalidate()
                patchList.repaint()
            }
        }, BorderLayout.BEFORE_FIRST_LINE)

        val textField = JTextField(15)
        var lastBuildVersion = LauncherProperties.getProperty("lastBuildVersion", "0.0.1")
        textField.text = lastBuildVersion
        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.X_AXIS)
        bottomPanel.add(textField, BorderLayout.WEST)
        bottomPanel.add(JButton("Create new build with patches").apply {
            addActionListener {
                if (lastBuildVersion != textField.text) {
                    lastBuildVersion = textField.text
                    LauncherProperties.setProperty("lastBuildVersion", lastBuildVersion)
                    LauncherProperties.store()
                }
                BuildManager.build(lastBuildVersion, currentPatchList)
            }
        }, BorderLayout.EAST)

        frame.add(bottomPanel, BorderLayout.AFTER_LAST_LINE)
        frame.isVisible = true
    }

    private fun addPatch(panel: JPanel, patch: Patch) {
        val checkBox = JCheckBox(patch.name.replace("_".toRegex(), " "))
        checkBox.isSelected = patch.isActive
        checkBox.addActionListener { patch.isActive = checkBox.isSelected }
        panel.add(checkBox)
    }

    private fun updatePatchesList(patchList: JPanel) {
        PatchManager.getPatches(true).also { currentPatchList = it }.forEach { addPatch(patchList, it) }
    }

    private object DarkLaf : FlatDarkLaf() {
        private fun readResolve(): Any = DarkLaf
        override fun getSupportsWindowDecorations(): Boolean {
            if (SystemInfo.isProjector || SystemInfo.isWebswing || SystemInfo.isWinPE) return false
            return !(SystemInfo.isWindows_10_orLater && FlatNativeWindowBorder.isSupported())
        }
    }
}
