package com.github.m9w.launcher.gui

import com.github.m9w.launcher.dao.Client
import com.github.m9w.launcher.dao.ClientFlags
import com.github.m9w.launcher.gui.GUI.ICON
import com.github.m9w.launcher.gui.components.JDropDown
import com.github.m9w.launcher.gui.components.JDropDownActions
import com.github.m9w.launcher.gui.components.JDropDownMulti
import com.github.m9w.launcher.launcher.ClientManager
import com.github.m9w.launcher.launcher.ClientManager.space
import com.github.m9w.launcher.properties.LauncherProperties
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.util.function.Consumer
import javax.swing.*
import javax.swing.JOptionPane.*

object Launcher : JPanel(MigLayout("wrap 2, ins 0", "5px[right]10px:push[grow,fill]5px", "5px[]8px[]5px")) {
    private fun readResolve(): Any = Launcher
    private val workdir = File(".").absolutePath.replace("[\\\\/.]*$".toRegex(), "")
    private val clientRows = ArrayList<ClientRow>()
    private var clients: JPanel

    init {
        val frame = JFrame("Darkbot launcher").apply {
            pack()
            setSize(1200, 800)
            iconImage = ICON
            setLocationRelativeTo(null)
            layout = BorderLayout()
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
            add(Launcher)
        }
        addButton("Launcher's dir", space.absolutePath.replace(workdir, ".")) {
            selectDir(space)?.let {
                space = it
                text = it.absolutePath.replace(workdir, ".")
                LauncherProperties.setProperty("space", space.absolutePath)
                LauncherProperties.store()
            }
        }
        addButton("Launcher builder") { Builder.isVisible = true }
        clients = addScrollPanel(1200) {
            layout = MigLayout("wrap 9, gap 0", "[][grow][][][][][][grow][]")
            add(JButton("#").apply {
                addActionListener {
                    if (clientRows.all { it.isSelected }) clientRows.forEach { it.isSelected = false }
                    else clientRows.forEach { it.isSelected = true }
                }
            })
            addColumn("Name", JTextField()) {
                client.name = it.text
            }
            addColumn("Version", JDropDown(ClientManager::getBuildVersions, ClientManager.getLastBuildVersion())) {
                client.version = it.getSelected()
            }
            addColumn("JVM tag", JTextField()) {
                client.jvmID = it.text
            }
            addColumn("Account", JDropDown(ClientManager::getAccounts)) {
                client.account = it.getSelected()
            }
            addColumn("Plugins", JDropDownMulti(emptySet(), ClientManager::getPlugins)) {
                client.plugins = it.getSelected().toMutableSet()
            }
            addColumn("Config", JDropDown(ClientManager::getConfigs)) {
                client.config = it.getSelected()
            }
            addColumn("Flags", JDropDownMulti(ClientFlags.toList())) {
                client.flags = it.getSelected().map(ClientFlags::valueOf).toMutableSet()
            }
            addColumn("Actions", JPanel(MigLayout("fillx, wrap 2")).apply {
                listOf(JDropDownActions.Actions.Start, JDropDownActions.Actions.Stop, JDropDownActions.Actions.Show,
                    JDropDownActions.Actions.Hide, JDropDownActions.Actions.Clone, JDropDownActions.Actions.Delete).forEach { action ->
                    addButton(text = action.name) { clientRows.filter { it.isSelected }.forEach { it.actionHandler(action) } }
                }
            }) {}
        }
        ClientManager.clients.forEach { ClientRow(clients, it) }
        addButton(text = "Add row") { ClientRow(clients).apply { focusOnName(); scrollDown() } }
        frame.isVisible = true
    }

    private fun <T : Component> Container.addColumn(title: String, component: T, action: ClientRow.(T) -> Unit) {
        add(JButton(title).apply { addActionListener {
            val rows = clientRows.filter { it.isSelected }
            if (rows.isEmpty()) showMessageDialog(parent, "Bulk operation available after selection necessary rows", "Error", 0)
            else {
                val option = JOptionPane(component, PLAIN_MESSAGE, OK_CANCEL_OPTION)
                option.createDialog(parent, "$title - bulk updating of selected rows").isVisible = true
                if (option.value == 0) rows.forEach { action.invoke(it, component) }
            }
        } }, "grow")
    }

    private fun JPanel.addButton(label: String? = null, text: String = "Open", action: JButton.() -> Unit): JButton {
        if (label != null) this@addButton.add(JLabel(label))
        return JButton().apply {
            this.text = text
            addActionListener { action.invoke(this) }
            this@addButton.add(this, if (label == null) "span 2, grow" else "")
        }
    }

    private fun addScrollPanel(height: Int, panel: JPanel.() -> Unit): JPanel {
        return JPanel(MigLayout("", "grow")).apply {
            panel.invoke(this)
            this@Launcher.add(JScrollPane(this).apply {
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(350, height)
            }, "span 2, growx")
        }
    }

    private fun selectDir(file: File = File(".")): File? = JFileChooser(file)
        .apply { setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY) }
        .also { it.showOpenDialog(this) }
        .selectedFile

    private class ClientRow(private var panel: JPanel, val client: ClientManager.ClientImpl = ClientManager.newClient()) {
        private val selected = JCheckBox()
        val name = JTextField(client.name)
        val version = JDropDown(ClientManager::getBuildVersions, client.version)
        val jvmId = JTextField(client.jvmID)
        val account = JDropDown(ClientManager::getAccounts, client.account)
        val plugins = JDropDownMulti(client.plugins, ClientManager::getPlugins)
        val config = JDropDown(ClientManager::getConfigs, client.config)
        val flags = JDropDownMulti(ClientFlags.toList(), client.flags.map { it.name })
        private val actions = JDropDownActions { actionHandler(it) }
        private var columns: List<Component> = listOf(selected, name, version, jvmId, account, plugins, config, flags, actions)

        var isSelected: Boolean get() = selected.isSelected; set(value) { selected.isSelected = value }

        init {
            columns.forEach { panel.add(it, "grow,h 30px") }
            name.onFocusLost { client.name = it }
            client.addListener(Client::name) { name.text = it }
            jvmId.onFocusLost { client.jvmID = it }
            client.addListener(Client::jvmID) { jvmId.text = it }
            version.onFocusLost { client.version = it }
            client.addListener(Client::version) { version.apply(it) }
            account.onFocusLost { client.account = it }
            client.addListener(Client::account) { account.apply(it) }
            config.onFocusLost { client.config = it }
            client.addListener(Client::config) { config.apply(it) }
            plugins.onFocusLost { client.plugins = plugins.getSelected().toMutableSet() }
            client.addListener(Client::plugins) { plugins.apply(it) }
            flags.onFocusLost { client.flags = flags.getSelected().map(ClientFlags::valueOf).toMutableSet() }
            client.addListener(Client::flags) { flags.apply(it.map { row -> row.name }) }
            clientRows.add(this)
            panel.revalidate()
        }

        fun focusOnName() = name.requestFocus()

        fun actionHandler(actions: JDropDownActions.Actions) {
            when (actions) {
                JDropDownActions.Actions.Start -> client.start()
                JDropDownActions.Actions.Stop -> client.stop()
                JDropDownActions.Actions.Show -> client.show()
                JDropDownActions.Actions.Hide -> client.hide()
                JDropDownActions.Actions.ExtractConfig -> client.extractConfig(showInputDialog(parent, "Enter config name:"))
                JDropDownActions.Actions.Clone -> ClientRow(clients, client.clone()).apply { focusOnName(); scrollDown() }
                JDropDownActions.Actions.Delete -> client.remove().let {
                    columns.forEach { panel.remove(it) }
                    clientRows.remove(this)
                    SwingUtilities.invokeLater { panel.revalidate(); panel.repaint() }
                }
                else -> {}
            }
        }
    }

    private fun JTextField.onFocusLost(listener: Consumer<String>) {
        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {}
            override fun focusLost(e: FocusEvent) = listener.accept(text)
        })
    }

    private fun scrollDown() = SwingUtilities.invokeLater { (clients.parent.parent as JScrollPane).verticalScrollBar.apply { value = maximum } }
}
