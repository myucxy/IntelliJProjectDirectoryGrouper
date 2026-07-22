package dev.projectgroups.directorygrouper.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.projectgroups.directorygrouper.DirectoryGrouperBundle
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class DirectoryGrouperConfigurable : Configurable {
    private var autoGroupMenusCheckBox: JBCheckBox? = null
    private var autoLoadGitBranchesCheckBox: JBCheckBox? = null

    override fun getDisplayName(): String = DISPLAY_NAME

    override fun createComponent(): JComponent {
        val groupingCheckBox = JBCheckBox(DirectoryGrouperBundle.message("settings.autoGroupMenus"))
        val gitBranchesCheckBox = JBCheckBox(
            DirectoryGrouperBundle.message("settings.autoLoadGitBranchesWhenMissing"),
        )
        autoGroupMenusCheckBox = groupingCheckBox
        autoLoadGitBranchesCheckBox = gitBranchesCheckBox

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
            add(groupingCheckBox.asSettingsRow())
            add(descriptionLabel("settings.autoGroupMenus.description"))
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(gitBranchesCheckBox.asSettingsRow())
            add(descriptionLabel("settings.autoLoadGitBranchesWhenMissing.description"))
        }
    }

    override fun isModified(): Boolean =
        autoGroupMenusCheckBox?.isSelected != DirectoryGrouperSettings.getInstance().state.autoGroupMenus ||
            autoLoadGitBranchesCheckBox?.isSelected !=
            DirectoryGrouperSettings.getInstance().state.autoLoadGitBranchesWhenMissing

    override fun apply() {
        autoGroupMenusCheckBox?.let {
            DirectoryGrouperSettings.getInstance().state.autoGroupMenus = it.isSelected
        }
        autoLoadGitBranchesCheckBox?.let {
            DirectoryGrouperSettings.getInstance().state.autoLoadGitBranchesWhenMissing = it.isSelected
        }
    }

    override fun reset() {
        autoGroupMenusCheckBox?.isSelected = DirectoryGrouperSettings.getInstance().state.autoGroupMenus
        autoLoadGitBranchesCheckBox?.isSelected =
            DirectoryGrouperSettings.getInstance().state.autoLoadGitBranchesWhenMissing
    }

    override fun disposeUIResources() {
        autoGroupMenusCheckBox = null
        autoLoadGitBranchesCheckBox = null
    }

    private fun JBCheckBox.asSettingsRow(): JBCheckBox = apply {
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private fun descriptionLabel(messageKey: String): JBLabel =
        JBLabel("<html>${DirectoryGrouperBundle.message(messageKey)}</html>").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(2, 24, 0, 0)
        }

    companion object {
        const val DISPLAY_NAME = "项目目录分组"
    }
}
