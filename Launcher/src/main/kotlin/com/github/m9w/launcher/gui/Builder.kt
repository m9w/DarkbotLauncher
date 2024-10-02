package com.github.m9w.launcher.gui

import com.github.m9w.launcher.builder.BuildManager
import com.github.m9w.launcher.gui.GUI.ICON
import com.github.m9w.launcher.patches.Patch
import com.github.m9w.launcher.patches.PatchManager
import com.github.m9w.launcher.properties.LauncherProperties
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

object Builder : JFrame("Darkbot launcher builder") {
    private fun readResolve(): Any = Builder

    private var currentPatchList: List<Patch> = emptyList()

    init {
        pack()
        setSize(350, 600)
        iconImage = ICON
        setLocationRelativeTo(null)
        layout = BorderLayout()

        val patchList = JPanel()
        patchList.layout = BoxLayout(patchList, BoxLayout.Y_AXIS)

        val scrollPane = JScrollPane(patchList)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.preferredSize = Dimension(350, 200)
        add(scrollPane, BorderLayout.CENTER)

        updatePatchesList(patchList)

        add(JButton("Store changes as patch").apply {
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

        add(bottomPanel, BorderLayout.AFTER_LAST_LINE)
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
}