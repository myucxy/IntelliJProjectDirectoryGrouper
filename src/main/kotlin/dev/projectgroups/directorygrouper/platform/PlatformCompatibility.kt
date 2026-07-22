package dev.projectgroups.directorygrouper.platform

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.Service

object PlatformActionIds {
    const val RECENT_PROJECTS = "RecentProjectListGroup"
    const val PROJECT_WIDGET = "main.toolbar.Project"
    const val PROJECT_WIDGET_ACTIONS = "ProjectWidget.Actions"
    const val OPEN_PROJECT_WINDOWS = "OpenProjectWindows"
    const val WELCOME_PROJECTS_QUICK_START = "WelcomeScreen.QuickStart.ProjectsState"
    const val WELCOME_RECENT_PROJECT_CONTEXT = "WelcomeScreenRecentProjectActionGroup"
}

data class PlatformCapabilities(
    val canReplaceRecentProjectsGroup: Boolean,
    val hasProjectWidget: Boolean,
    val canWrapOpenProjectsGroup: Boolean,
    val hasWelcomeQuickStartGroup: Boolean,
    val hasWelcomeRecentProjectContextGroup: Boolean,
)

@Service(Service.Level.APP)
class PlatformCompatibility {
    fun detect(): PlatformCapabilities {
        val actionManager = ActionManager.getInstance()
        return PlatformCapabilities(
            canReplaceRecentProjectsGroup = actionManager.isActionGroup(PlatformActionIds.RECENT_PROJECTS),
            hasProjectWidget = actionManager.getAction(PlatformActionIds.PROJECT_WIDGET) != null,
            canWrapOpenProjectsGroup = actionManager.isActionGroup(PlatformActionIds.OPEN_PROJECT_WINDOWS),
            hasWelcomeQuickStartGroup = actionManager.isActionGroup(PlatformActionIds.WELCOME_PROJECTS_QUICK_START),
            hasWelcomeRecentProjectContextGroup = actionManager.isActionGroup(
                PlatformActionIds.WELCOME_RECENT_PROJECT_CONTEXT,
            ),
        )
    }

    private fun ActionManager.isActionGroup(actionId: String): Boolean = getAction(actionId) is ActionGroup
}
