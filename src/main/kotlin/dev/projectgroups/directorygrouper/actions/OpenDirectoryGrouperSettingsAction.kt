package dev.projectgroups.directorygrouper.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import dev.projectgroups.directorygrouper.DirectoryGrouperBundle
import dev.projectgroups.directorygrouper.settings.DirectoryGrouperConfigurable

class OpenDirectoryGrouperSettingsAction : DumbAwareAction(
    DirectoryGrouperBundle.message("action.openSettings"),
    DirectoryGrouperBundle.message("action.openSettings.description"),
    AllIcons.General.Settings,
) {
    override fun actionPerformed(event: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            event.project,
            DirectoryGrouperConfigurable::class.java,
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
